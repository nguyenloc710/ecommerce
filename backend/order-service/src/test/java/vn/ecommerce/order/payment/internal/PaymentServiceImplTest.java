package vn.ecommerce.order.payment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import vn.ecommerce.common.web.error.ForbiddenException;
import vn.ecommerce.common.web.security.HmacSignatures;
import vn.ecommerce.order.orders.OrdersApi;
import vn.ecommerce.order.payment.PaymentInitResult;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.PaymentRequest;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final String SECRET = "test-secret";
    private static final UUID ORDER = UUID.randomUUID();

    @Mock PaymentRepository payments;
    @Mock OrdersApi orders;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        PaymentProperties props = new PaymentProperties(new PaymentProperties.Mock(SECRET, "http://localhost:3000/thanh-toan/mock/{orderId}?ma={orderCode}"), null);
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of(new CodGateway(), new MockOnlineGateway(props, JsonMapper.builder().build())));
        service = new PaymentServiceImpl(payments, registry, orders);
    }

    @Test
    void initiate_mockOnline_returnsPayUrlWithOrderId_andPendingStatus() {
        when(payments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentInitResult result = service.initiate(new PaymentRequest(ORDER, "ORD-1", new BigDecimal("430000"), PaymentMethod.MOCK_ONLINE, null));

        assertThat(result.payUrl()).isEqualTo("http://localhost:3000/thanh-toan/mock/" + ORDER + "?ma=ORD-1");
        assertThat(result.status()).isEqualTo("PENDING");
    }

    @Test
    void initiate_cod_hasNoPayUrl() {
        when(payments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentInitResult result = service.initiate(new PaymentRequest(ORDER, "ORD-1", new BigDecimal("430000"), PaymentMethod.COD, null));

        assertThat(result.payUrl()).isNull();
        assertThat(result.status()).isEqualTo("INIT");
    }

    @Test
    void webhook_invalidSignature_isForbidden_andNothingChanges() {
        String body = body("txn-1", "SUCCESS", "430000");

        assertThatThrownBy(() -> service.handleWebhook(PaymentMethod.MOCK_ONLINE, "deadbeef", body))
                .isInstanceOf(ForbiddenException.class).hasMessage(PaymentErrorKeys.INVALID_SIGNATURE);
        verifyNoInteractions(payments, orders);
    }

    @Test
    void webhook_success_settlesPayment_andConfirmsOrder() {
        Payment pending = new Payment(ORDER, PaymentMethod.MOCK_ONLINE, new BigDecimal("430000"), Payment.PaymentStatus.PENDING, "http://pay");
        when(payments.findByProviderAndProviderTxnId(PaymentMethod.MOCK_ONLINE, "txn-1")).thenReturn(Optional.empty());
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(ORDER)).thenReturn(Optional.of(pending));
        String body = body("txn-1", "SUCCESS", "430000");

        service.handleWebhook(PaymentMethod.MOCK_ONLINE, HmacSignatures.sign(SECRET, body), body);

        assertThat(pending.getStatus()).isEqualTo(Payment.PaymentStatus.SUCCEEDED);
        assertThat(pending.getProviderTxnId()).isEqualTo("txn-1");
        assertThat(pending.getPaidAt()).isNotNull();
        verify(orders).markPaid(ORDER, "txn-1");
    }

    @Test
    void webhook_duplicateTxn_isIgnored() {
        when(payments.findByProviderAndProviderTxnId(PaymentMethod.MOCK_ONLINE, "txn-1"))
                .thenReturn(Optional.of(new Payment(ORDER, PaymentMethod.MOCK_ONLINE, BigDecimal.TEN, Payment.PaymentStatus.SUCCEEDED, null)));
        String body = body("txn-1", "SUCCESS", "430000");

        service.handleWebhook(PaymentMethod.MOCK_ONLINE, HmacSignatures.sign(SECRET, body), body);

        verify(payments, never()).save(any());
        verifyNoInteractions(orders);
    }

    @Test
    void webhook_amountMismatch_isTreatedAsFailure() {
        Payment pending = new Payment(ORDER, PaymentMethod.MOCK_ONLINE, new BigDecimal("430000"), Payment.PaymentStatus.PENDING, "http://pay");
        when(payments.findByProviderAndProviderTxnId(any(), any())).thenReturn(Optional.empty());
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(ORDER)).thenReturn(Optional.of(pending));
        String body = body("txn-2", "SUCCESS", "1000");

        service.handleWebhook(PaymentMethod.MOCK_ONLINE, HmacSignatures.sign(SECRET, body), body);

        assertThat(pending.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        verify(orders).markPaymentFailed(ORDER, PaymentServiceImpl.REASON_AMOUNT_MISMATCH);
        verify(orders, never()).markPaid(any(), any());
    }

    @Test
    void webhook_failed_cancelsOrderWithReason() {
        Payment pending = new Payment(ORDER, PaymentMethod.MOCK_ONLINE, new BigDecimal("430000"), Payment.PaymentStatus.PENDING, "http://pay");
        when(payments.findByProviderAndProviderTxnId(any(), any())).thenReturn(Optional.empty());
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(ORDER)).thenReturn(Optional.of(pending));
        String body = "{\"orderId\":\"" + ORDER + "\",\"txnId\":\"txn-3\",\"status\":\"FAILED\",\"amount\":430000,\"reason\":\"insufficient-funds\"}";

        service.handleWebhook(PaymentMethod.MOCK_ONLINE, HmacSignatures.sign(SECRET, body), body);

        verify(orders).markPaymentFailed(ORDER, "insufficient-funds");
    }

    private static String body(String txn, String status, String amount) {
        return "{\"orderId\":\"" + ORDER + "\",\"txnId\":\"" + txn + "\",\"status\":\"" + status + "\",\"amount\":" + amount + "}";
    }
}
