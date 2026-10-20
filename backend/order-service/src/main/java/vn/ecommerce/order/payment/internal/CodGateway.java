package vn.ecommerce.order.payment.internal;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.ecommerce.order.payment.PaymentMethod;

/** Thu tiền khi giao: không có cổng, không có webhook. */
@Component
class CodGateway implements PaymentGateway {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.COD;
    }

    @Override
    public GatewayInit initiate(UUID orderId, String orderCode, BigDecimal amount) {
        return new GatewayInit(null, Payment.PaymentStatus.INIT);
    }

    @Override
    public boolean verify(String signature, String rawBody) {
        return false;
    }

    @Override
    public WebhookNotification parse(String rawBody) {
        throw new UnsupportedOperationException("COD không có webhook");
    }
}
