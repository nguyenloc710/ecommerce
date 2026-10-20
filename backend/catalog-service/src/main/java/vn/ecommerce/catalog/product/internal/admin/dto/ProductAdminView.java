package vn.ecommerce.catalog.product.internal.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Chi tiết sản phẩm cho admin: mọi trạng thái, kèm variant và ảnh. */
public record ProductAdminView(
        UUID id,
        UUID categoryId,
        UUID brandId,
        String slug,
        String name,
        String description,
        Map<String, Object> attributes,
        String pricingType,
        String status,
        String seoTitle,
        String seoDescription,
        Instant publishedAt,
        Instant updatedAt,
        String updatedBy,
        long version,
        List<VariantAdminView> variants,
        List<ImageAdminView> images) {

    public record VariantAdminView(UUID id, String skuCode, Map<String, String> optionValues, BigDecimal price,
                                   BigDecimal compareAtPrice, Integer weightGram, String barcode, String status,
                                   int sortOrder, long version) {
    }

    public record ImageAdminView(UUID id, UUID variantId, String url, String alt, int sortOrder) {
    }
}
