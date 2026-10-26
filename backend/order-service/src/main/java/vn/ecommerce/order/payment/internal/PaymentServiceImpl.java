package vn.ecommerce.order.payment.internal;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.web.error.ForbiddenException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.order.orders.OrdersApi;
import vn.ecommerce.order.orders.PaymentOutcome;
import vn.ecommerce.order.payment.PaymentInitResult;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.PaymentRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class PaymentServiceImpl implements PaymentService {

    static final String REASON_AMOUNT_MISMATCH = Payment.REASON_AMOUNT_MISMATCH;

    private final PaymentRepository payments;
    private final PaymentGatewayRegistry gateways;
    private final OrdersApi orders;
    private final RefundService refunds;

    @Override
    public PaymentInitResult initiate(PaymentRequest request) {
        PaymentGateway.GatewayInit init = gateways.require(request.method()).initiate(request);
        Payment payment = payments.save(new Payment(request.orderId(), request.method(), request.amount(), init.initialStatus(), init.payUrl()));
        return new PaymentInitResult(payment.getId(), request.method(), payment.getStatus().name(), payment.getPayUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PaymentMethod> availableMethods() {
        return gateways.methods();
    }

    @Override
    public void handleWebhook(PaymentMethod provider, String signature, String rawBody) {
        PaymentGateway gateway = gateways.require(provider);
        if (!gateway.verify(signature, rawBody)) {
            log.warn("Webhook {} có chữ ký sai", provider);
            throw new ForbiddenException(PaymentErrorKeys.INVALID_SIGNATURE);
        }
        PaymentGateway.WebhookNotification note = gateway.parse(rawBody);
        MDC.put("orderId", String.valueOf(note.orderId()));
        MDC.put("paymentProvider", provider.name());
        try {
            if (payments.findByProviderAndProviderTxnId(provider, note.txnId()).isPresent()) {
                log.info("Webhook {} giao dịch {} đã xử lý, bỏ qua", provider, note.txnId());
                return;
            }
            Payment payment = payments.findFirstByOrderIdOrderByCreatedAtDesc(note.orderId())
                    .orElseThrow(() -> new NotFoundException(PaymentErrorKeys.NOT_FOUND));
            if (payment.isSettled()) {
                log.info("Payment của đơn {} đã {}; bỏ qua webhook {}", note.orderId(), payment.getStatus(), note.txnId());
                return;
            }
            boolean success = note.success() && amountMatches(payment, note);
            String reason = note.success() && !success ? REASON_AMOUNT_MISMATCH : note.reason();
            payment.settle(success, note.txnId(), raw(note, reason));
            payments.save(payment);
            PaymentOutcome outcome = success
                    ? orders.markPaid(note.orderId(), note.txnId())
                    : orders.markPaymentFailed(note.orderId(), reason == null ? "declined" : reason);
            if (outcome.orderCancelled()) {
                refunds.openForCancelledWebhook(payment, outcome.orderCode());
            }
            log.info("Webhook {} giao dịch {}: {}", provider, note.txnId(), success ? "thành công" : "thất bại (" + reason + ")");
        } finally {
            MDC.remove("orderId");
            MDC.remove("paymentProvider");
        }
    }

    private static boolean amountMatches(Payment payment, PaymentGateway.WebhookNotification note) {
        boolean ok = note.amount() != null && note.amount().compareTo(payment.getAmount()) == 0;
        if (!ok) {
            log.warn("Số tiền webhook {} khác số tiền đơn {}", note.amount(), payment.getAmount());
        }
        return ok;
    }

    private static Map<String, Object> raw(PaymentGateway.WebhookNotification note, String reason) {
        Map<String, Object> raw = new HashMap<>();
        raw.put("txnId", note.txnId());
        raw.put("success", note.success());
        raw.put(Payment.RAW_AMOUNT, note.amount());
        raw.put(Payment.RAW_REASON, reason);
        return raw;
    }
}
