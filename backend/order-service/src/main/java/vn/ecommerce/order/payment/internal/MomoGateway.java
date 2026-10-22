package vn.ecommerce.order.payment.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import vn.ecommerce.common.web.security.HmacSignatures;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.PaymentRequest;

/**
 * Cổng MoMo, API v2 (E6.2). Khởi tạo gọi {@code POST /v2/gateway/api/create} lấy payUrl; kết quả chính thức đến qua IPN
 * {@code POST /webhooks/payments/momo} (JSON, chữ ký nằm trong body, MoMo cần HTTP 204).
 *
 * <p>Lời gọi tạo giao dịch nằm trong transaction tạo đơn của checkout: MoMo lỗi thì đơn rollback và nhả hàng, không có đơn
 * online thiếu link thanh toán. Đổi lại giữ kết nối DB trong lúc chờ MoMo (timeout theo {@code spring.http.clients}).
 * Chỉ có bean khi {@code ecommerce.payment.momo.enabled=true} (P-03).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ecommerce.payment.momo", name = "enabled", havingValue = "true")
class MomoGateway implements PaymentGateway {

    static final String CREATE_PATH = "/v2/gateway/api/create";
    static final int RESULT_SUCCESS = 0;
    static final String LANG = "vi";

    private final PaymentProperties.Momo cfg;
    private final RestClient client;
    private final ObjectMapper json;

    MomoGateway(PaymentProperties properties, RestClient.Builder builder, ObjectMapper json) {
        this.cfg = properties.momo();
        this.client = builder.baseUrl(cfg.endpoint()).build();
        this.json = json;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOMO;
    }

    @Override
    public GatewayInit initiate(PaymentRequest request) {
        String requestId = UUID.randomUUID().toString();
        long amount = request.amount().setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        String orderInfo = "Thanh toan don hang " + request.orderCode();
        String redirectUrl = cfg.redirectUrlTemplate().replace("{orderCode}", request.orderCode());
        String extraData = "";
        String raw = "accessKey=" + cfg.accessKey() + "&amount=" + amount + "&extraData=" + extraData + "&ipnUrl=" + cfg.ipnUrl()
                + "&orderId=" + request.orderId() + "&orderInfo=" + orderInfo + "&partnerCode=" + cfg.partnerCode()
                + "&redirectUrl=" + redirectUrl + "&requestId=" + requestId + "&requestType=" + cfg.requestType();
        CreateRequest body = new CreateRequest(cfg.partnerCode(), requestId, amount, request.orderId().toString(), orderInfo, redirectUrl,
                cfg.ipnUrl(), cfg.requestType(), extraData, LANG, HmacSignatures.sign(cfg.secretKey(), raw));
        CreateResponse res;
        try {
            res = client.post().uri(CREATE_PATH).body(body).retrieve().body(CreateResponse.class);
        } catch (RestClientException e) {
            log.warn("Gọi MoMo tạo giao dịch thất bại: {}", e.getMessage());
            throw new PaymentGatewayUnavailableException(PaymentMethod.MOMO.name());
        }
        if (res == null || res.resultCode() != RESULT_SUCCESS || res.payUrl() == null) {
            log.warn("MoMo từ chối tạo giao dịch: resultCode={}, message={}", res == null ? null : res.resultCode(), res == null ? null : res.message());
            throw new PaymentGatewayUnavailableException(PaymentMethod.MOMO.name());
        }
        return new GatewayInit(res.payUrl(), Payment.PaymentStatus.PENDING);
    }

    /** Header chữ ký không dùng: MoMo đặt {@code signature} trong body, ký trên các trường theo thứ tự chữ cái. */
    @Override
    public boolean verify(String signature, String rawBody) {
        Ipn ipn;
        try {
            ipn = json.readValue(rawBody, Ipn.class);
        } catch (JacksonException e) {
            // Body không phải IPN hợp lệ: coi như sai chữ ký (403), không để thành lỗi 500
            return false;
        }
        String raw = "accessKey=" + cfg.accessKey() + "&amount=" + ipn.amount() + "&extraData=" + nullToEmpty(ipn.extraData())
                + "&message=" + nullToEmpty(ipn.message()) + "&orderId=" + ipn.orderId() + "&orderInfo=" + nullToEmpty(ipn.orderInfo())
                + "&orderType=" + nullToEmpty(ipn.orderType()) + "&partnerCode=" + ipn.partnerCode() + "&payType=" + nullToEmpty(ipn.payType())
                + "&requestId=" + ipn.requestId() + "&responseTime=" + ipn.responseTime() + "&resultCode=" + ipn.resultCode()
                + "&transId=" + ipn.transId();
        return cfg.partnerCode().equals(ipn.partnerCode()) && HmacSignatures.matches(cfg.secretKey(), raw, ipn.signature());
    }

    @Override
    public WebhookNotification parse(String rawBody) {
        Ipn ipn = json.readValue(rawBody, Ipn.class);
        boolean success = ipn.resultCode() == RESULT_SUCCESS;
        return new WebhookNotification(UUID.fromString(ipn.orderId()), String.valueOf(ipn.transId()), success, BigDecimal.valueOf(ipn.amount()),
                success ? null : "momo-" + ipn.resultCode());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    record CreateRequest(String partnerCode, String requestId, long amount, String orderId, String orderInfo, String redirectUrl, String ipnUrl,
                         String requestType, String extraData, String lang, String signature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CreateResponse(int resultCode, String message, String payUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ipn(String partnerCode, String orderId, String requestId, long amount, String orderInfo, String orderType, long transId, int resultCode,
               String message, String payType, long responseTime, String extraData, String signature) {
    }
}
