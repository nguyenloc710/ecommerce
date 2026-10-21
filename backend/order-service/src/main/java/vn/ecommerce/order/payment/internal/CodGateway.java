package vn.ecommerce.order.payment.internal;

import org.springframework.stereotype.Component;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.PaymentRequest;

/** Thu tiền khi giao: không có cổng, không có webhook. */
@Component
class CodGateway implements PaymentGateway {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.COD;
    }

    @Override
    public GatewayInit initiate(PaymentRequest request) {
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
