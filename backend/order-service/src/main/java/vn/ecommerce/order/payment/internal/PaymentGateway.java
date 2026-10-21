package vn.ecommerce.order.payment.internal;

import java.math.BigDecimal;
import java.util.UUID;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.PaymentRequest;

/**
 * Strategy cho từng cổng thanh toán. Thêm cổng = thêm bean; {@link PaymentGatewayRegistry} gom theo {@link PaymentMethod}.
 * Webhook đi theo Template Method trong {@code PaymentServiceImpl}: verify chữ ký → idempotency → parse → áp dụng.
 */
public interface PaymentGateway {

    PaymentMethod method();

    /** Khởi tạo giao dịch ở cổng; COD không có URL. */
    GatewayInit initiate(PaymentRequest request);

    /** Kiểm tra chữ ký trên nội dung thô (body JSON, hoặc query string với VNPay). Cổng không có webhook (COD) trả false. */
    boolean verify(String signature, String rawBody);

    WebhookNotification parse(String rawBody);

    record GatewayInit(String payUrl, Payment.PaymentStatus initialStatus) {
    }

    /** @param reason lý do thất bại từ cổng (null khi thành công) */
    record WebhookNotification(UUID orderId, String txnId, boolean success, BigDecimal amount, String reason) {
    }
}
