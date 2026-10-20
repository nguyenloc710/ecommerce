package vn.ecommerce.catalog.product.internal;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;
import vn.ecommerce.catalog.product.internal.domain.VariantStatus;
import vn.ecommerce.catalog.product.internal.repo.ImageRow;
import vn.ecommerce.catalog.product.internal.repo.ProductImageRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductVariantRepository;
import vn.ecommerce.catalog.product.internal.repo.VariantInfoRow;
import vn.ecommerce.common.jpa.InBatches;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class VariantLookupServiceImpl implements VariantLookupApi {

    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    @Override
    public List<VariantInfo> findVariants(Collection<UUID> skuIds) {
        List<VariantInfoRow> rows = InBatches.query(skuIds, variantRepository::findInfoRowsByIdIn);
        List<UUID> productIds = rows.stream().map(VariantInfoRow::productId).distinct().toList();
        List<ImageRow> images = InBatches.query(productIds, imageRepository::findRowsByProductIdIn);
        // Ưu tiên ảnh gắn đúng variant, không có thì ảnh đầu tiên của sản phẩm
        Map<UUID, String> byVariant = images.stream().filter(i -> i.variantId() != null)
                .collect(Collectors.toMap(ImageRow::variantId, ImageRow::url, (first, later) -> first));
        Map<UUID, String> byProduct = images.stream()
                .collect(Collectors.toMap(ImageRow::productId, ImageRow::url, (first, later) -> first));
        return rows.stream().map(r -> new VariantInfo(
                r.skuId(), r.productId(), r.productSlug(), r.productName(), r.skuCode(), r.optionValues(),
                r.price(), r.compareAtPrice(),
                byVariant.getOrDefault(r.skuId(), byProduct.get(r.productId())),
                r.variantStatus() == VariantStatus.ACTIVE && r.productStatus() == ProductStatus.ACTIVE)).toList();
    }

    @Override
    public List<UUID> searchVariantIds(String q, int limit) {
        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return variantRepository.searchIds(pattern, PageRequest.of(0, limit));
    }
}
