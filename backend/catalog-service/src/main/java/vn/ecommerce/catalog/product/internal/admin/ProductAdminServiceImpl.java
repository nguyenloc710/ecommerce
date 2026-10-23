package vn.ecommerce.catalog.product.internal.admin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.internal.Slugs;
import vn.ecommerce.catalog.product.internal.admin.dto.ImageItemCommand;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminQuery;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminRow;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductUpsertCommand;
import vn.ecommerce.catalog.product.internal.admin.dto.VariantUpsertCommand;
import vn.ecommerce.catalog.product.internal.domain.Brand;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.GoldKarat;
import vn.ecommerce.catalog.product.internal.domain.PricingType;
import vn.ecommerce.catalog.product.internal.domain.Product;
import vn.ecommerce.catalog.product.internal.domain.ProductImage;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.catalog.product.internal.domain.VariantStatus;
import vn.ecommerce.catalog.product.internal.repo.BrandRepository;
import vn.ecommerce.catalog.product.internal.repo.CategoryRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductImageRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRepository;
import vn.ecommerce.catalog.product.internal.gold.GoldPricing;
import vn.ecommerce.catalog.product.internal.repo.ProductVariantRepository;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class ProductAdminServiceImpl implements ProductAdminService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final AdminMapper mapper;
    private final CurrentUser currentUser;
    private final GoldPricing goldPricing;

    @Override
    @Transactional(readOnly = true)
    public PageView<ProductAdminRow> page(ProductAdminQuery query) {
        String q = query.q() == null || query.q().isBlank() ? null : "%" + query.q().trim().toLowerCase(Locale.ROOT) + "%";
        boolean anyStatus = query.status() == null || query.status().isBlank();
        ProductStatus status = anyStatus ? ProductStatus.DRAFT : ProductStatus.valueOf(query.status());
        boolean anyCategory = query.categoryId() == null;
        UUID categoryId = anyCategory ? new UUID(0L, 0L) : query.categoryId();
        Page<ProductAdminRow> page = productRepository.pageAdmin(q, anyStatus, status, anyCategory, categoryId,
                PageRequest.of(query.page(), query.size()));
        return new PageView<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAdminView get(UUID productId) {
        return view(load(productId));
    }

    @Override
    public ProductAdminView create(ProductUpsertCommand command) {
        String slug = Slugs.resolve(command.slug(), command.name());
        if (productRepository.existsBySlug(slug)) {
            throw new ConflictException(AdminErrorKeys.PRODUCT_SLUG_TAKEN, slug);
        }
        Product product = new Product(resolveCategory(command.categoryId()), command.name(), slug);
        apply(product, command);
        Product saved = productRepository.save(product);
        log.info("Tạo sản phẩm {} ({}) trạng thái {}", saved.getSlug(), saved.getId(), saved.getStatus());
        return view(saved);
    }

    @Override
    public ProductAdminView update(UUID productId, ProductUpsertCommand command) {
        Product product = load(productId);
        VersionCheck.require(product, command.version());
        String slug = Slugs.resolve(command.slug(), command.name());
        if (productRepository.existsBySlugAndIdNot(slug, productId)) {
            throw new ConflictException(AdminErrorKeys.PRODUCT_SLUG_TAKEN, slug);
        }
        product.setCategory(resolveCategory(command.categoryId()));
        product.setName(command.name());
        product.setSlug(slug);
        apply(product, command);
        if (product.getPricingType() == PricingType.GOLD_RATE) {
            // Chuyển sang giá theo vàng: variant đã đủ trường giá vàng được tính lại ngay, variant thiếu giữ giá cũ tới khi sửa
            variantRepository.findByProductIdOrderBySortOrderAsc(productId).stream().filter(ProductVariant::hasGoldPricing)
                    .forEach(goldPricing::applyCurrentRate);
        }
        return view(productRepository.save(product));
    }

    @Override
    public void delete(UUID productId) {
        Product product = load(productId);
        String by = currentUser.requireId();
        List<ProductVariant> variants = variantRepository.findByProductIdOrderBySortOrderAsc(productId);
        variants.forEach(v -> v.markDeleted(by));
        variantRepository.saveAll(variants);
        product.markDeleted(by);
        productRepository.save(product);
        log.info("Xóa mềm sản phẩm {} ({}) cùng {} variant", product.getSlug(), productId, variants.size());
    }

    @Override
    public ProductAdminView addVariant(UUID productId, VariantUpsertCommand command) {
        Product product = load(productId);
        String sku = command.skuCode().trim();
        if (variantRepository.existsBySkuCode(sku)) {
            throw new ConflictException(AdminErrorKeys.VARIANT_SKU_TAKEN, sku);
        }
        ProductVariant variant = new ProductVariant(product, sku, command.price());
        applyVariant(variant, command);
        priceByGold(product, variant);
        variantRepository.save(variant);
        return view(product);
    }

    @Override
    public ProductAdminView updateVariant(UUID productId, UUID variantId, VariantUpsertCommand command) {
        Product product = load(productId);
        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new NotFoundException(AdminErrorKeys.VARIANT_NOT_FOUND));
        VersionCheck.require(variant, command.version());
        String sku = command.skuCode().trim();
        if (variantRepository.existsBySkuCodeAndIdNot(sku, variantId)) {
            throw new ConflictException(AdminErrorKeys.VARIANT_SKU_TAKEN, sku);
        }
        variant.setSkuCode(sku);
        variant.setPrice(command.price());
        applyVariant(variant, command);
        priceByGold(product, variant);
        variantRepository.save(variant);
        return view(product);
    }

    @Override
    public ProductAdminView deleteVariant(UUID productId, UUID variantId) {
        Product product = load(productId);
        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new NotFoundException(AdminErrorKeys.VARIANT_NOT_FOUND));
        variant.markDeleted(currentUser.requireId());
        variantRepository.save(variant);
        return view(product);
    }

    @Override
    public ProductAdminView replaceImages(UUID productId, List<ImageItemCommand> images) {
        Product product = load(productId);
        Map<UUID, ProductVariant> variantsById = variantRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Map<UUID, ProductImage> existingById = imageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .collect(Collectors.toMap(ProductImage::getId, Function.identity()));

        // Validate hết trước khi ghi: ảnh gắn variant thì variant phải thuộc sản phẩm này
        for (ImageItemCommand item : images) {
            if (item.variantId() != null && !variantsById.containsKey(item.variantId())) {
                throw new BadRequestException(AdminErrorKeys.IMAGE_VARIANT_MISMATCH);
            }
        }

        List<ProductImage> toSave = new ArrayList<>();
        Set<UUID> keptIds = new java.util.HashSet<>();
        for (int order = 0; order < images.size(); order++) {
            ImageItemCommand item = images.get(order);
            ProductImage image = item.id() == null ? null : existingById.get(item.id());
            if (image == null) {
                image = new ProductImage(product, item.url(), order);
            }
            image.setUrl(item.url());
            image.setAlt(item.alt());
            image.setSortOrder(order);
            image.setVariant(item.variantId() == null ? null : variantsById.get(item.variantId()));
            toSave.add(image);
            keptIds.add(image.getId());
        }
        List<ProductImage> toDelete = existingById.values().stream().filter(i -> !keptIds.contains(i.getId())).toList();
        imageRepository.deleteAllInBatch(toDelete);
        imageRepository.saveAll(toSave);
        log.info("Thay ảnh sản phẩm {}: giữ/thêm {}, xóa {}", productId, toSave.size(), toDelete.size());
        return view(product);
    }

    // ---- helpers ----

    private Product load(UUID productId) {
        return productRepository.findWithRelationsById(productId)
                .orElseThrow(() -> new NotFoundException(AdminErrorKeys.PRODUCT_NOT_FOUND_ID));
    }

    private Category resolveCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException(AdminErrorKeys.CATEGORY_NOT_FOUND_ID));
    }

    private Brand resolveBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new BadRequestException(AdminErrorKeys.BRAND_NOT_FOUND_ID));
    }

    /** Gán các trường chung; lần đầu chuyển sang ACTIVE thì ghi {@code publishedAt}. */
    private void apply(Product product, ProductUpsertCommand command) {
        product.setBrand(resolveBrand(command.brandId()));
        product.setDescription(command.description());
        product.setAttributes(command.attributes() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(command.attributes()));
        product.setPricingType(command.pricingType() == null ? PricingType.FIXED : PricingType.valueOf(command.pricingType()));
        product.setSeoTitle(command.seoTitle());
        product.setSeoDescription(command.seoDescription());
        ProductStatus status = command.status() == null ? ProductStatus.DRAFT : ProductStatus.valueOf(command.status());
        product.setStatus(status);
        if (status == ProductStatus.ACTIVE && product.getPublishedAt() == null) {
            product.setPublishedAt(Instant.now());
        }
    }

    private static void applyVariant(ProductVariant variant, VariantUpsertCommand command) {
        variant.setOptionValues(command.optionValues() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(command.optionValues()));
        variant.setCompareAtPrice(command.compareAtPrice());
        variant.setWeightGram(command.weightGram());
        variant.setBarcode(command.barcode());
        variant.setStatus(command.status() == null ? VariantStatus.ACTIVE : VariantStatus.valueOf(command.status()));
        variant.setSortOrder(command.sortOrder() == null ? 0 : command.sortOrder());
        variant.setGoldKarat(command.goldKarat() == null ? null : GoldKarat.valueOf(command.goldKarat()));
        variant.setGoldWeight(command.goldWeight());
        variant.setMakingFee(command.makingFee());
    }

    /** Sản phẩm GOLD_RATE: giá variant luôn tính từ giá vàng hiện hành, bỏ qua giá client gửi (B-09). */
    private void priceByGold(Product product, ProductVariant variant) {
        if (product.getPricingType() == PricingType.GOLD_RATE) {
            goldPricing.applyCurrentRate(variant);
        }
    }

    private ProductAdminView view(Product product) {
        List<ProductAdminView.VariantAdminView> variants = variantRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream().map(mapper::toVariantView).toList();
        List<ProductAdminView.ImageAdminView> images = imageRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream().map(mapper::toImageView).toList();
        return mapper.toProductView(product, variants, images);
    }
}
