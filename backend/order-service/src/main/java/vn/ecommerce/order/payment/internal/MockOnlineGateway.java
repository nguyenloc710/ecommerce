package vn.ecommerce.order.payment.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import vn.ecommerce.common.web.security.HmacSignatures;
import vn.ecommerce.order.payment.PaymentMethod;

/**
 * Cổng giả (B-18): URL trỏ về trang thanh toán giả trên storefront; webhook body
 * {@code {"orderId","txnId","status":"SUCCESS|FAILED","amount","reason"}} ký HMAC-SHA256 bằng secret cấu hình.
 */
@Component
@RequiredArgsConstructor
class MockOnlineGateway implements PaymentGateway {

    static final String STATUS_SUCCESS = "SUCCESS";

    private final PaymentProperties properties;
    private final ObjectMapper json;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOCK_ONLINE;
    }

    @Override
    public GatewayInit initiate(UUID orderId, String orderCode, BigDecimal amount) {
        String url = properties.mock().payUrlTemplate().replace("{orderId}", orderId.toString());
        return new GatewayInit(url, Payment.PaymentStatus.PENDING);
    }

    @Override
    public boolean verify(String signature, String rawBody) {
        return HmacSignatures.matches(properties.mock().secret(), rawBody, signature);
    }

    @Override
    public WebhookNotification parse(String rawBody) {
        MockPayload p = json.readValue(rawBody, MockPayload.class);
        return new WebhookNotification(p.orderId(), p.txnId(), STATUS_SUCCESS.equalsIgnoreCase(p.status()), p.amount(), p.reason());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MockPayload(UUID orderId, String txnId, String status, BigDecimal amount, String reason) {
    }
}
