package vn.ecommerce.catalog.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Chi tiết sản phẩm cho PDP. {@code options} được tính từ {@code optionValues} của các variant. */
public record ProductDetailView(
        UUID id,
        String slug,
        String name,
        String description,
        String pricingType,
        Map<String, Object> attributes,
        BrandRef brand,
        CategoryRef category,
        List<CategoryRef> breadcrumb,
        List<VariantView> variants,
        List<ImageView> images,
        List<OptionView> options) {

    public record BrandRef(UUID id, String name, String slug) {
    }

    public record CategoryRef(UUID id, String name, String slug) {
    }

    public record VariantView(UUID id, String skuCode, Map<String, String> optionValues, BigDecimal price,
                              BigDecimal compareAtPrice, Integer weightGram) {
    }

    public record ImageView(UUID id, UUID variantId, String url, String alt, int sortOrder) {
    }

    public record OptionView(String name, List<String> values) {
    }
}
