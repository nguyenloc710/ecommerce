package vn.ecommerce.order.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Giỏ đã ghép giá/tồn kho hiện tại từ catalog. Giá không lưu trong giỏ: mỗi lần xem là tra lại.
 *
 * @param issues cảnh báo cho khách (dòng hết hàng, ngừng bán...) để FE hiển thị, không chặn xem giỏ
 */
public record CartView(List<Line> lines, BigDecimal subtotal, int totalQuantity, List<Issue> issues) {

    public record Line(UUID skuId, String productSlug, String productName, String variantLabel, String imageUrl,
                       BigDecimal unitPrice, BigDecimal compareAtPrice, int quantity, String engraving,
                       BigDecimal lineTotal, int available, boolean sellable) {
    }

    /** {@code code} là message key (ví dụ {@code inventory.insufficient}) để FE dịch; {@code message} đã dịch sẵn. */
    public record Issue(UUID skuId, String code, String message) {
    }
}
