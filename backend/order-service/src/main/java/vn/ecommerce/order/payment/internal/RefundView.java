package vn.ecommerce.order.payment.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.ecommerce.order.payment.PaymentMethod;

public record RefundView(
        UUID id,
        String orderCode,
        PaymentMethod provider,
        String providerTxnId,
        BigDecimal amount,
        PaymentRefund.RefundCause cause,
        String cancelReason,
        PaymentRefund.RefundStatus status,
        String reference,
        String note,
        Instant createdAt,
        Instant completedAt,
        String completedBy,
        long version) {

    static RefundView of(PaymentRefund r) {
        return new RefundView(r.getId(), r.getOrderCode(), r.getProvider(), r.getProviderTxnId(), r.getAmount(), r.getCause(),
                r.getCancelReason(), r.getStatus(), r.getReference(), r.getNote(), r.getCreatedAt(), r.getCompletedAt(),
                r.getStatus() == PaymentRefund.RefundStatus.DONE ? r.getUpdatedBy() : null, r.getVersion());
    }
}
