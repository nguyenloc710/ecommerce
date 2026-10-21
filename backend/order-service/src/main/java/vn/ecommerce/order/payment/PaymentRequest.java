package vn.ecommerce.order.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Yêu cầu khởi tạo thanh toán cho một đơn.
 *
 * @param clientIp IP trình duyệt của khách (VNPay bắt buộc gửi kèm); storefront chuyển tiếp, có thể null
 */
public record PaymentRequest(UUID orderId, String orderCode, BigDecimal amount, PaymentMethod method, String clientIp) {
}
