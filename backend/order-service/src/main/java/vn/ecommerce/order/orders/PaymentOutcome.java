package vn.ecommerce.order.orders;

/**
 * Kết quả báo kết quả thanh toán cho đơn.
 *
 * @param orderCode      mã đơn hiển thị
 * @param orderCancelled đơn đang ở CANCELLED sau khi xử lý (đã hủy từ trước, hoặc vừa hủy vì thanh toán thất bại);
 *                       nếu khách đã bị trừ tiền thì payment mở yêu cầu hoàn tiền (B-33)
 */
public record PaymentOutcome(String orderCode, boolean orderCancelled) {
}
