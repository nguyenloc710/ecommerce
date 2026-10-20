package vn.ecommerce.order.checkout;

import vn.ecommerce.order.orders.OrderView;

/**
 * Kết quả đặt hàng.
 *
 * @param payUrl  URL sang trang thanh toán với cổng online; null với COD
 * @param replayed true khi Idempotency-Key đã dùng: trả lại đơn cũ, không tạo đơn mới
 */
public record CheckoutResult(OrderView order, String payUrl, boolean replayed) {
}
