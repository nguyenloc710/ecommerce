package vn.ecommerce.order.payment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ForbiddenException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.order.payment.PaymentMethod;

@ExtendWith(MockitoExtension.class)
class VnpayIpnHandlerTest {

    private static final String QUERY = "vnp_TxnRef=abc&vnp_ResponseCode=00&vnp_SecureHash=DEADBEEF";

    @Mock PaymentService paymentService;
    @InjectMocks VnpayIpnHandler handler;

    @Test
    void processed_returns00_andPassesSignatureFromQuery() {
        assertThat(handler.handle(QUERY)).isEqualTo(VnpayIpnHandler.IpnReply.CONFIRMED);
        verify(paymentService).handleWebhook(PaymentMethod.VNPAY, "DEADBEEF", QUERY);
    }

    @Test
    void invalidSignature_returns97() {
        doThrow(new ForbiddenException(PaymentErrorKeys.INVALID_SIGNATURE)).when(paymentService).handleWebhook(eq(PaymentMethod.VNPAY), any(), any());

        assertThat(handler.handle(QUERY).code()).isEqualTo("97");
    }

    @Test
    void unknownOrder_returns01() {
        doThrow(new NotFoundException(PaymentErrorKeys.NOT_FOUND)).when(paymentService).handleWebhook(eq(PaymentMethod.VNPAY), any(), any());

        assertThat(handler.handle(QUERY).code()).isEqualTo("01");
    }

    @Test
    void otherBusinessError_returns99() {
        doThrow(new BadRequestException(PaymentErrorKeys.METHOD_NOT_SUPPORTED, "VNPAY")).when(paymentService).handleWebhook(eq(PaymentMethod.VNPAY), any(), any());

        assertThat(handler.handle(QUERY).code()).isEqualTo("99");
    }
}
