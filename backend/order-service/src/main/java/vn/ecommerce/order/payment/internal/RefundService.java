package vn.ecommerce.order.payment.internal;

import java.util.UUID;
import vn.ecommerce.common.web.error.PageResponse;

/** Hoàn tiền tay cho đơn đã hủy nhưng đã nhận tiền (B-33). */
public interface RefundService {

    /** Đơn bị hủy: mở yêu cầu hoàn nếu payment mới nhất đã trừ tiền khách ({@link Payment#moneyReceived()}); idempotent theo payment. */
    void openForCancelledOrder(UUID orderId, String orderCode, String cancelReason);

    /** Webhook xong mà đơn ở CANCELLED (tiền về muộn, hoặc sai số tiền): mở yêu cầu hoàn nếu khách đã bị trừ tiền; idempotent theo payment. */
    void openForCancelledWebhook(Payment payment, String orderCode);

    /** Danh sách theo trạng thái, cũ nhất trước (chờ lâu nhất xử lý trước). */
    PageResponse<RefundView> list(PaymentRefund.RefundStatus status, int page, int size);

    /**
     * Đánh dấu đã hoàn (ghi admin_audit_log).
     *
     * @throws vn.ecommerce.common.web.error.NotFoundException {@code payment.refund-not-found}
     * @throws vn.ecommerce.common.web.error.ConflictException {@code common.concurrent-modification} khi {@code version} lệch,
     *                                                         {@code payment.refund-already-done}
     */
    RefundView complete(UUID id, long version, String reference, String note);
}
