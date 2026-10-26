package vn.ecommerce.order.payment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.order.payment.PaymentMethod;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    private static final UUID ORDER = UUID.randomUUID();

    @Mock PaymentRepository payments;
    @Mock PaymentRefundRepository refunds;
    @Mock AdminAuditRecorder audit;
    @InjectMocks RefundServiceImpl service;

    private static Payment payment(boolean success, String amount) {
        Payment p = new Payment(ORDER, PaymentMethod.VNPAY, new BigDecimal("430000"), Payment.PaymentStatus.PENDING, "http://pay");
        String reason = success && !"430000".equals(amount) ? Payment.REASON_AMOUNT_MISMATCH : (success ? null : "declined");
        Map<String, Object> raw = new java.util.HashMap<>();
        raw.put(Payment.RAW_AMOUNT, new BigDecimal(amount));
        raw.put(Payment.RAW_REASON, reason);
        p.settle(success && reason == null, "txn-1", raw);
        return p;
    }

    private PaymentRefund captureSaved() {
        ArgumentCaptor<PaymentRefund> saved = ArgumentCaptor.forClass(PaymentRefund.class);
        verify(refunds).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void cancelledOrder_withSucceededPayment_opensRefund() {
        Payment paid = payment(true, "430000");
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(ORDER)).thenReturn(Optional.of(paid));

        service.openForCancelledOrder(ORDER, "ORD-1", "customer-cancel");

        PaymentRefund refund = captureSaved();
        assertThat(refund.getCause()).isEqualTo(PaymentRefund.RefundCause.CANCELLED_AFTER_PAID);
        assertThat(refund.getAmount()).isEqualByComparingTo("430000");
        assertThat(refund.getOrderCode()).isEqualTo("ORD-1");
        assertThat(refund.getCancelReason()).isEqualTo("customer-cancel");
        assertThat(refund.getStatus()).isEqualTo(PaymentRefund.RefundStatus.PENDING);
    }

    @Test
    void cancelledOrder_codOrFailedPayment_opensNothing() {
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(ORDER)).thenReturn(Optional.of(payment(false, "430000")));

        service.openForCancelledOrder(ORDER, "ORD-1", "payment-failed:declined");

        verify(refunds, never()).save(any());
    }

    @Test
    void amountMismatch_refundsAmountActuallyPaid() {
        Payment mismatch = payment(true, "1000");

        service.openForCancelledWebhook(mismatch, "ORD-1");

        PaymentRefund refund = captureSaved();
        assertThat(refund.getCause()).isEqualTo(PaymentRefund.RefundCause.AMOUNT_MISMATCH);
        assertThat(refund.getAmount()).isEqualByComparingTo("1000");
    }

    @Test
    void latePayment_opensRefund_once() {
        Payment paid = payment(true, "430000");
        when(refunds.existsByPaymentId(paid.getId())).thenReturn(false, true);

        service.openForCancelledWebhook(paid, "ORD-1");
        service.openForCancelledOrder(ORDER, "ORD-1", "payment-timeout");

        assertThat(captureSaved().getCause()).isEqualTo(PaymentRefund.RefundCause.PAID_AFTER_CANCEL);
    }

    @Test
    void complete_marksDone_andAudits() {
        PaymentRefund refund = new PaymentRefund(payment(true, "430000"), "ORD-1", PaymentRefund.RefundCause.PAID_AFTER_CANCEL, null);
        when(refunds.findById(refund.getId())).thenReturn(Optional.of(refund));

        RefundView view = service.complete(refund.getId(), 0, "RF-123", "đã hoàn qua VNPay");

        assertThat(view.status()).isEqualTo(PaymentRefund.RefundStatus.DONE);
        assertThat(view.reference()).isEqualTo("RF-123");
        assertThat(view.completedAt()).isNotNull();
        verify(refunds).saveAndFlush(refund);
        verify(audit).record(eq(RefundServiceImpl.AUDIT_REFUND_DONE), eq(RefundServiceImpl.AUDIT_TARGET), eq(refund.getId().toString()), anyMap());
    }

    @Test
    void complete_staleVersion_isConflict_andNothingChanges() {
        PaymentRefund refund = new PaymentRefund(payment(true, "430000"), "ORD-1", PaymentRefund.RefundCause.PAID_AFTER_CANCEL, null);
        when(refunds.findById(refund.getId())).thenReturn(Optional.of(refund));

        assertThatThrownBy(() -> service.complete(refund.getId(), 3, "RF-1", null))
                .isInstanceOf(ConflictException.class).hasMessage(CommonErrorKeys.CONCURRENT_MODIFICATION);
        assertThat(refund.getStatus()).isEqualTo(PaymentRefund.RefundStatus.PENDING);
        verify(audit, never()).record(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void complete_alreadyDone_isConflict() {
        PaymentRefund refund = new PaymentRefund(payment(true, "430000"), "ORD-1", PaymentRefund.RefundCause.PAID_AFTER_CANCEL, null);
        refund.complete("RF-1", null, java.time.Instant.now());
        when(refunds.findById(refund.getId())).thenReturn(Optional.of(refund));

        assertThatThrownBy(() -> service.complete(refund.getId(), 0, "RF-2", null))
                .isInstanceOf(ConflictException.class).hasMessage(PaymentErrorKeys.REFUND_ALREADY_DONE);
        assertThat(refund.getReference()).isEqualTo("RF-1");
        verify(refunds, never()).saveAndFlush(any());
    }

    @Test
    void complete_unknownId_isNotFound() {
        UUID id = UUID.randomUUID();
        when(refunds.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(id, 0, "RF-1", null))
                .isInstanceOf(NotFoundException.class).hasMessage(PaymentErrorKeys.REFUND_NOT_FOUND);
    }
}
