package vn.ecommerce.order.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu đã tính xong từ checkout (giá tra lại từ catalog, giảm giá, phí ship, hàng đã giữ) để module orders ghi đơn.
 *
 * @param orderId            id đã dùng để giữ hàng ở catalog
 * @param confirmImmediately COD: xác nhận ngay; thanh toán online: chờ webhook
 */
public record PlaceOrderCommand(
        UUID orderId,
        UUID customerId,
        String idempotencyKey,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal discount,
        String voucherCode,
        BigDecimal shippingFee,
        String shippingCarrier,
        String paymentMethod,
        boolean confirmImmediately,
        ShippingAddress address,
        String contactEmail,
        String note) {

    public BigDecimal total() {
        return subtotal.subtract(discount).add(shippingFee);
    }

    public record Line(UUID skuId, UUID productId, String productSlug, String productName, String variantLabel,
                       String skuCode, String imageUrl, BigDecimal unitPrice, int quantity, String engraving) {

        public BigDecimal lineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
