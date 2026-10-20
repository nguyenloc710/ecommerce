package vn.ecommerce.catalog.product;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Thông tin bán hàng của một SKU, dùng cho module khác (inventory, lookup nội bộ cho order-service).
 *
 * @param sellable  variant và sản phẩm đều ACTIVE, chưa xóa
 * @param imageUrl  ảnh theo variant nếu có, không thì ảnh đầu của sản phẩm; null nếu chưa có ảnh
 */
public record VariantInfo(UUID skuId, UUID productId, String productSlug, String productName, String skuCode,
                          Map<String, String> optionValues, BigDecimal price, BigDecimal compareAtPrice,
                          String imageUrl, boolean sellable) {

    /** Nhãn hiển thị cho dòng giỏ/đơn, ví dụ {@code Bạc / 6}. */
    public String variantLabel() {
        return String.join(" / ", optionValues.values());
    }
}
