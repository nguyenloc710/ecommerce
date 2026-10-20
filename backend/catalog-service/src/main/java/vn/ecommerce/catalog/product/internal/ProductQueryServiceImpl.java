package vn.ecommerce.catalog.product.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.product.CategoryTreeNode;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.ProductCardView;
import vn.ecommerce.catalog.product.ProductDetailView;
import vn.ecommerce.catalog.product.ProductListQuery;
import vn.ecommerce.catalog.product.ProductQueryService;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.CategoryStatus;
import vn.ecommerce.catalog.product.internal.domain.Product;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.catalog.product.internal.domain.VariantStatus;
import vn.ecommerce.catalog.product.internal.repo.BrandRepository;
import vn.ecommerce.catalog.product.internal.repo.CategoryRepository;
import vn.ecommerce.catalog.product.internal.repo.CategoryRow;
import vn.ecommerce.catalog.product.internal.repo.ImageRow;
import vn.ecommerce.catalog.product.internal.repo.ProductImageRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRow;
import vn.ecommerce.catalog.product.internal.repo.ProductVariantRepository;
import vn.ecommerce.catalog.product.internal.repo.VariantRow;
import vn.ecommerce.common.jpa.InBatches;
import vn.ecommerce.common.web.error.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ProductQueryServiceImpl implements ProductQueryService {

    /** Mã nhãn, FE dịch sang chữ hiển thị (không trả chuỗi tiếng Việt từ API). */
    private static final String LABEL_NEW = "NEW";
    /** UUID giả cho tham số không dùng tới (xem ghi chú ở {@link ProductRepository}). */
    private static final UUID UNUSED_ID = new UUID(0L, 0L);

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductMapper mapper;
    private final ProductProperties properties;

    @Override
    public List<CategoryTreeNode> categoryTree() {
        return buildTree(activeCategories());
    }

    @Override
    public PageView<ProductCardView> listProducts(ProductListQuery query) {
        List<CategoryRow> categories = activeCategories();
        Set<UUID> categoryIds = resolveCategorySubtree(query.categorySlug(), categories);
        UUID brandId = resolveBrandId(query.brandSlug());

        Page<ProductRow> page = pageProducts(query, categoryIds, brandId);
        List<UUID> productIds = page.getContent().stream().map(ProductRow::id).toList();

        Map<UUID, List<VariantRow>> variantsByProduct = InBatches.query(productIds, variantRepository::findActiveRowsByProductIdIn)
                .stream().collect(Collectors.groupingBy(VariantRow::productId, LinkedHashMap::new, Collectors.toList()));
        Map<UUID, String> firstImageByProduct = InBatches.query(productIds, imageRepository::findRowsByProductIdIn)
                .stream().collect(Collectors.toMap(ImageRow::productId, ImageRow::url, (first, later) -> first));

        Instant newSince = Instant.now().minus(properties.newLabelFor());
        List<ProductCardView> cards = page.getContent().stream()
                .map(row -> toCard(row, variantsByProduct.getOrDefault(row.id(), List.of()), firstImageByProduct.get(row.id()), newSince))
                .toList();
        return new PageView<>(cards, page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext());
    }

    @Override
    public ProductDetailView productDetail(String slug) {
        Product product = productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException(ProductErrorKeys.PRODUCT_NOT_FOUND));
        List<ProductVariant> variants = variantRepository.findByProductIdAndStatusOrderBySortOrderAsc(product.getId(), VariantStatus.ACTIVE);
        List<ProductDetailView.ImageView> images = imageRepository.findRowsByProductIdIn(List.of(product.getId())).stream()
                .map(row -> new ProductDetailView.ImageView(row.id(), row.variantId(), row.url(), row.alt(), row.sortOrder()))
                .toList();

        return new ProductDetailView(
                product.getId(), product.getSlug(), product.getName(), product.getDescription(),
                product.getPricingType().name(), product.getAttributes(),
                product.getBrand() == null ? null : mapper.toBrandRef(product.getBrand()),
                mapper.toCategoryRef(product.getCategory()),
                breadcrumb(product.getCategory()),
                variants.stream().map(mapper::toVariantView).toList(),
                images,
                options(variants));
    }

    // ---- danh mục ----

    private List<CategoryRow> activeCategories() {
        return categoryRepository.findRowsByStatus(CategoryStatus.ACTIVE);
    }

    private static List<CategoryTreeNode> buildTree(List<CategoryRow> rows) {
        Map<UUID, List<CategoryRow>> childrenByParent = rows.stream()
                .filter(r -> r.parentId() != null)
                .collect(Collectors.groupingBy(CategoryRow::parentId));
        return rows.stream()
                .filter(r -> r.parentId() == null)
                .map(r -> toNode(r, childrenByParent))
                .toList();
    }

    private static CategoryTreeNode toNode(CategoryRow row, Map<UUID, List<CategoryRow>> childrenByParent) {
        List<CategoryTreeNode> children = childrenByParent.getOrDefault(row.id(), List.of()).stream()
                .map(child -> toNode(child, childrenByParent))
                .toList();
        return new CategoryTreeNode(row.id(), row.name(), row.slug(), row.sortOrder(), children);
    }

    /** Danh mục theo slug và toàn bộ con cháu; rỗng nghĩa là không lọc. */
    private static Set<UUID> resolveCategorySubtree(String slug, List<CategoryRow> categories) {
        if (slug == null || slug.isBlank()) {
            return Set.of();
        }
        CategoryRow root = categories.stream().filter(c -> c.slug().equals(slug)).findFirst()
                .orElseThrow(() -> new NotFoundException(ProductErrorKeys.CATEGORY_NOT_FOUND, slug));
        Map<UUID, List<CategoryRow>> childrenByParent = categories.stream()
                .filter(c -> c.parentId() != null)
                .collect(Collectors.groupingBy(CategoryRow::parentId));
        Set<UUID> ids = new LinkedHashSet<>();
        Deque<CategoryRow> queue = new ArrayDeque<>(List.of(root));
        while (!queue.isEmpty()) {
            CategoryRow current = queue.poll();
            ids.add(current.id());
            queue.addAll(childrenByParent.getOrDefault(current.id(), List.of()));
        }
        return ids;
    }

    private UUID resolveBrandId(String brandSlug) {
        if (brandSlug == null || brandSlug.isBlank()) {
            return null;
        }
        return brandRepository.findIdBySlug(brandSlug)
                .orElseThrow(() -> new NotFoundException(ProductErrorKeys.BRAND_NOT_FOUND, brandSlug));
    }

    private List<ProductDetailView.CategoryRef> breadcrumb(Category leaf) {
        Deque<ProductDetailView.CategoryRef> chain = new ArrayDeque<>();
        for (Category c = leaf; c != null; c = c.getParent()) {
            chain.addFirst(mapper.toCategoryRef(c));
        }
        return List.copyOf(chain);
    }

    // ---- sản phẩm ----

    private Page<ProductRow> pageProducts(ProductListQuery query, Set<UUID> categoryIds, UUID brandId) {
        boolean allCategories = categoryIds.isEmpty();
        boolean anyBrand = brandId == null;
        // Tham số "không dùng" vẫn phải có giá trị hợp lệ để driver suy được kiểu (xem ProductRepository)
        List<UUID> ids = allCategories ? List.of(UNUSED_ID) : List.copyOf(categoryIds);
        UUID brand = anyBrand ? UNUSED_ID : brandId;
        Pageable pageable = PageRequest.of(query.page(), query.size());
        return switch (query.sort()) {
            case NEWEST -> productRepository.pageNewest(allCategories, ids, anyBrand, brand, pageable);
            case PRICE_ASC -> productRepository.pagePriceAsc(allCategories, ids, anyBrand, brand, pageable);
            case PRICE_DESC -> productRepository.pagePriceDesc(allCategories, ids, anyBrand, brand, pageable);
        };
    }

    private ProductCardView toCard(ProductRow row, List<VariantRow> variants, String imageUrl, Instant newSince) {
        Optional<VariantRow> cheapest = variants.stream().min(Comparator.comparing(VariantRow::price));
        BigDecimal price = cheapest.map(VariantRow::price).orElse(null);
        BigDecimal compareAt = cheapest.map(VariantRow::compareAtPrice)
                .filter(c -> price != null && c.compareTo(price) > 0)
                .orElse(null);
        List<String> colors = variants.stream()
                .map(v -> v.optionValues().get(properties.colorOption()))
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();
        String label = row.publishedAt() != null && row.publishedAt().isAfter(newSince) ? LABEL_NEW : null;
        return new ProductCardView(row.id(), row.slug(), row.name(), price, compareAt, imageUrl, colors, label);
    }

    /** Gom option từ variant theo thứ tự xuất hiện: {size: [S, M, L], color: [Đen, Trắng]}. */
    private static List<ProductDetailView.OptionView> options(List<ProductVariant> variants) {
        Map<String, LinkedHashSet<String>> values = new LinkedHashMap<>();
        for (ProductVariant variant : variants) {
            variant.getOptionValues().forEach((name, value) ->
                    values.computeIfAbsent(name, Function.<String>identity().andThen(k -> new LinkedHashSet<>())).add(value));
        }
        List<ProductDetailView.OptionView> result = new ArrayList<>();
        values.forEach((name, set) -> result.add(new ProductDetailView.OptionView(name, List.copyOf(set))));
        return result;
    }
}
