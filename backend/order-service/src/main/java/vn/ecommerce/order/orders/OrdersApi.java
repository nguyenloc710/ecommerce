package vn.ecommerce.order.orders;

import java.util.Optional;
import java.util.UUID;

/** API public của module orders cho checkout (tạo đơn) và payment (kết quả thanh toán). */
public interface OrdersApi {

    /**
     * Ghi đơn PENDING rồi chuyển CONFIRMED (COD) hoặc AWAITING_PAYMENT (online) trong cùng transaction;
     * phát {@code order.confirmed} qua outbox khi xác nhận.
     */
    OrderView place(PlaceOrderCommand command);

    /** Đơn đã tạo với cùng Idempotency-Key của khách (gọi lại trả đơn cũ, không tạo thêm). */
    Optional<OrderView> findByIdempotencyKey(UUID customerId, String idempotencyKey);

    /** Thanh toán online thành công: AWAITING_PAYMENT → CONFIRMED. Đơn đã CONFIRMED trở đi: bỏ qua (idempotent). */
    void markPaid(UUID orderId, String providerTxnId);

    /** Thanh toán thất bại: AWAITING_PAYMENT → CANCELLED (catalog nhả hàng qua event). */
    void markPaymentFailed(UUID orderId, String reason);
}
