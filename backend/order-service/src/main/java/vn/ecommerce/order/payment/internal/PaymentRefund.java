package vn.ecommerce.order.payment.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;
import vn.ecommerce.order.payment.PaymentMethod;

/**
 * Yêu cầu hoàn tiền tay (B-33): đơn đã hủy nhưng đã nhận tiền online. Shop hoàn ở trang quản trị của cổng
 * rồi đánh dấu xong ở đây; không gọi API hoàn tiền của cổng.
 */
@Getter
@Entity
@Table(name = "payment_refund")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRefund extends AuditableEntity {

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "order_code", nullable = false, updatable = false, length = 30)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentMethod provider;

    @Column(name = "provider_txn_id", updatable = false, length = 100)
    private String providerTxnId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private RefundCause cause;

    @Column(name = "cancel_reason", updatable = false)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(length = 100)
    private String reference;

    @Column(length = 255)
    private String note;

    @Column(name = "completed_at")
    private Instant completedAt;

    PaymentRefund(Payment payment, String orderCode, RefundCause cause, String cancelReason) {
        this.paymentId = payment.getId();
        this.orderId = payment.getOrderId();
        this.orderCode = orderCode;
        this.provider = payment.getProvider();
        this.providerTxnId = payment.getProviderTxnId();
        this.amount = payment.receivedAmount();
        this.cause = cause;
        this.cancelReason = cancelReason;
        this.status = RefundStatus.PENDING;
    }

    /** @return false nếu đã hoàn trước đó (không ghi lại). */
    boolean complete(String reference, String note, Instant at) {
        if (status == RefundStatus.DONE) {
            return false;
        }
        this.status = RefundStatus.DONE;
        this.reference = reference;
        this.note = note;
        this.completedAt = at;
        return true;
    }

    public enum RefundCause {
        /** Tiền về sau khi đơn đã hủy (hết hạn thanh toán, B-21). */
        PAID_AFTER_CANCEL,
        /** Cổng báo thành công nhưng số tiền khác số tiền đơn: đơn bị hủy, hoàn đúng số khách đã trả. */
        AMOUNT_MISMATCH,
        /** Khách hoặc shop hủy đơn đã thanh toán online. */
        CANCELLED_AFTER_PAID
    }

    public enum RefundStatus {
        PENDING,
        DONE
    }
}
