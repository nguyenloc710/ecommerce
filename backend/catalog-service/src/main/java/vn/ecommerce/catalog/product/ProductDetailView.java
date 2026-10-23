package vn.ecommerce.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
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

    /**
     * @param priceLabel   mã nhãn khi giá đang được điều chỉnh (ví dụ {@code FLASH_SALE}), null nếu giá gốc
     * @param priceEndsAt  giá điều chỉnh hết hiệu lực lúc nào (FE đếm ngược)
     * @param gold         cách tính giá theo vàng (sản phẩm GOLD_RATE, B-09); null với giá cố định
     */
    public record VariantView(UUID id, String skuCode, Map<String, String> optionValues, BigDecimal price,
                              BigDecimal compareAtPrice, Integer weightGram, String priceLabel, Instant priceEndsAt,
                              GoldBreakdown gold) {

        /** Bản sao với giá điều chỉnh (giá gốc thành giá gạch). */
        public VariantView adjusted(BigDecimal newPrice, BigDecimal newCompareAt, String label, Instant endsAt) {
            return new VariantView(id, skuCode, optionValues, newPrice, newCompareAt, weightGram, label, endsAt, gold);
        }
    }

    /**
     * Giá = {@code weightGram × ratePerGram + makingFee}, làm tròn.
     *
     * @param rateEffectiveAt giá vàng được nhập lúc nào ("giá vàng cập nhật 09:00 23/10")
     */
    public record GoldBreakdown(String karat, BigDecimal weightGram, BigDecimal ratePerGram, BigDecimal makingFee, Instant rateEffectiveAt) {
    }

    public record ImageView(UUID id, UUID variantId, String url, String alt, int sortOrder) {
    }

    public record OptionView(String name, List<String> values) {
    }
}
