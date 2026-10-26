package vn.ecommerce.order.payment.internal;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.common.web.error.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class RefundServiceImpl implements RefundService {

    static final String AUDIT_REFUND_DONE = "PAYMENT_REFUND_DONE";
    static final String AUDIT_TARGET = "PAYMENT_REFUND";

    private final PaymentRepository payments;
    private final PaymentRefundRepository refunds;
    private final AdminAuditRecorder audit;

    @Override
    public void openForCancelledOrder(UUID orderId, String orderCode, String cancelReason) {
        payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .filter(Payment::moneyReceived)
                .ifPresent(p -> open(p, orderCode, p.amountMismatch() ? PaymentRefund.RefundCause.AMOUNT_MISMATCH
                        : PaymentRefund.RefundCause.CANCELLED_AFTER_PAID, cancelReason));
    }

    @Override
    public void openForCancelledWebhook(Payment payment, String orderCode) {
        if (payment.moneyReceived()) {
            open(payment, orderCode, payment.amountMismatch() ? PaymentRefund.RefundCause.AMOUNT_MISMATCH
                    : PaymentRefund.RefundCause.PAID_AFTER_CANCEL, null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RefundView> list(PaymentRefund.RefundStatus status, int page, int size) {
        Page<PaymentRefund> result = refunds.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(page, size));
        return PageResponse.of(result.map(RefundView::of).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public RefundView complete(UUID id, long version, String reference, String note) {
        PaymentRefund refund = refunds.findById(id).orElseThrow(() -> new NotFoundException(PaymentErrorKeys.REFUND_NOT_FOUND));
        if (refund.getVersion() != version) {
            throw new ConflictException(CommonErrorKeys.CONCURRENT_MODIFICATION);
        }
        if (!refund.complete(reference, note, Instant.now())) {
            throw new ConflictException(PaymentErrorKeys.REFUND_ALREADY_DONE, refund.getOrderCode());
        }
        refunds.saveAndFlush(refund);
        Map<String, Object> details = new HashMap<>();
        details.put("orderCode", refund.getOrderCode());
        details.put("amount", refund.getAmount());
        details.put("reference", reference);
        audit.record(AUDIT_REFUND_DONE, AUDIT_TARGET, id.toString(), details);
        log.info("Đã hoàn tiền đơn {} ({} {})", refund.getOrderCode(), refund.getProvider(), refund.getAmount());
        return RefundView.of(refund);
    }

    private void open(Payment payment, String orderCode, PaymentRefund.RefundCause cause, String cancelReason) {
        if (refunds.existsByPaymentId(payment.getId())) {
            log.info("Đơn {} đã có yêu cầu hoàn tiền, bỏ qua", orderCode);
            return;
        }
        refunds.save(new PaymentRefund(payment, orderCode, cause, cancelReason));
        log.warn("Mở yêu cầu hoàn tiền đơn {} ({}, {} {})", orderCode, cause, payment.getProvider(), payment.getAmount());
    }
}
