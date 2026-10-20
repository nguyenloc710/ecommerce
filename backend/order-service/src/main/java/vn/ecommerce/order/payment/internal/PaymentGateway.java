package vn.ecommerce.order.payment.internal;

import java.math.BigDecimal;
import java.util.UUID;
import vn.ecommerce.order.payment.PaymentMethod;

/**
 * Strategy cho từng cổng thanh toán. Thêm cổng = thêm bean; {@link PaymentGatewayRegistry} gom theo {@link PaymentMethod}.
 * Webhook đi theo Template Method trong {@code PaymentServiceImpl}: verify chữ ký → idempotency → parse → áp dụng.
 */
public interface PaymentGateway {

    PaymentMethod method();

    /** Khởi tạo giao dịch ở cổng; COD không có URL. */
    GatewayInit initiate(UUID orderId, String orderCode, BigDecimal amount);

    /** Kiểm tra chữ ký webhook trên body thô. Cổng không có webhook (COD) trả false. */
    boolean verify(String signature, String rawBody);

    WebhookNotification parse(String rawBody);

    record GatewayInit(String payUrl, Payment.PaymentStatus initialStatus) {
    }

    /** @param reason lý do thất bại từ cổng (null khi thành công) */
    record WebhookNotification(UUID orderId, String txnId, boolean success, BigDecimal amount, String reason) {
    }
}
