package vn.ecommerce.order.payment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import vn.ecommerce.common.web.security.HmacSignatures;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.PaymentRequest;

/** Chữ ký IPN đối chiếu với HMAC-SHA256 tính độc lập bằng Node crypto trên đúng chuỗi MoMo quy định. */
class MomoGatewayTest {

    private static final String SECRET = "momo-secret";
    private static final UUID ORDER = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final String IPN_SIGNATURE = "f019ce74a50966f9193227b4d617c81bb03938d7764b8644dbdf3480f0fabaf7";
    private static final String IPN = """
            {"partnerCode":"MOMOTEST","orderId":"0f8fad5b-d9cb-469f-a165-70867728950e","requestId":"r-1","amount":890000,
             "orderInfo":"Thanh toan don hang ORD-261021-0008","orderType":"momo_wallet","transId":4088878653,"resultCode":0,
             "message":"Thành công.","payType":"qr","responseTime":1761036000000,"extraData":"","signature":"%s"}""";

    private final ObjectMapper json = JsonMapper.builder().build();
    private MockRestServiceServer server;
    private MomoGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        PaymentProperties props = new PaymentProperties(null, null, new PaymentProperties.Momo(true, "MOMOTEST", "ACCESSKEY", SECRET,
                "https://test-payment.momo.vn", "captureWallet", "http://localhost:3000/thanh-toan/ket-qua/{orderCode}",
                "https://api.example.vn/webhooks/payments/momo"));
        gateway = new MomoGateway(props, builder, json);
    }

    @Test
    void initiate_postsSignedCreateRequest_andReturnsPayUrl() {
        AtomicReference<String> sent = new AtomicReference<>();
        server.expect(requestTo("https://test-payment.momo.vn/v2/gateway/api/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(req -> sent.set(((MockClientHttpRequest) req).getBodyAsString()))
                .andRespond(withSuccess("{\"resultCode\":0,\"message\":\"Thành công.\",\"payUrl\":\"https://test-payment.momo.vn/pay/abc\"}",
                        MediaType.APPLICATION_JSON));

        var init = gateway.initiate(new PaymentRequest(ORDER, "ORD-261021-0008", new BigDecimal("890000.00"), PaymentMethod.MOMO, "203.0.113.7"));

        assertThat(init.payUrl()).isEqualTo("https://test-payment.momo.vn/pay/abc");
        assertThat(init.initialStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        JsonNode body = json.readTree(sent.get());
        assertThat(body.get("amount").asLong()).isEqualTo(890000);
        assertThat(body.get("orderId").asString()).isEqualTo(ORDER.toString());
        assertThat(body.get("redirectUrl").asString()).isEqualTo("http://localhost:3000/thanh-toan/ket-qua/ORD-261021-0008");
        String raw = "accessKey=ACCESSKEY&amount=890000&extraData=&ipnUrl=https://api.example.vn/webhooks/payments/momo&orderId=" + ORDER
                + "&orderInfo=Thanh toan don hang ORD-261021-0008&partnerCode=MOMOTEST&redirectUrl=http://localhost:3000/thanh-toan/ket-qua/ORD-261021-0008"
                + "&requestId=" + body.get("requestId").asString() + "&requestType=captureWallet";
        assertThat(body.get("signature").asString()).isEqualTo(HmacSignatures.sign(SECRET, raw));
        server.verify();
    }

    @Test
    void initiate_momoRejects_throwsGatewayUnavailable() {
        server.expect(requestTo("https://test-payment.momo.vn/v2/gateway/api/create"))
                .andRespond(withSuccess("{\"resultCode\":11,\"message\":\"Truy cập bị từ chối.\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.initiate(new PaymentRequest(ORDER, "ORD-1", new BigDecimal("1000"), PaymentMethod.MOMO, null)))
                .isInstanceOf(PaymentGatewayUnavailableException.class)
                .hasMessage(PaymentErrorKeys.GATEWAY_UNAVAILABLE);
    }

    @Test
    void initiate_momoDown_throwsGatewayUnavailable() {
        server.expect(requestTo("https://test-payment.momo.vn/v2/gateway/api/create")).andRespond(withServerError());

        assertThatThrownBy(() -> gateway.initiate(new PaymentRequest(ORDER, "ORD-1", new BigDecimal("1000"), PaymentMethod.MOMO, null)))
                .isInstanceOf(PaymentGatewayUnavailableException.class);
    }

    @Test
    void verify_acceptsIpnSignedByMomo_ignoringHeader() {
        assertThat(gateway.verify(null, IPN.formatted(IPN_SIGNATURE))).isTrue();
    }

    @Test
    void verify_rejectsTamperedAmount_wrongPartner_andGarbage() {
        assertThat(gateway.verify(null, IPN.formatted(IPN_SIGNATURE).replace("\"amount\":890000", "\"amount\":1000"))).isFalse();
        assertThat(gateway.verify(null, IPN.formatted(IPN_SIGNATURE).replace("MOMOTEST", "OTHER"))).isFalse();
        assertThat(gateway.verify(null, "{\"orderId\":\"x\"")).isFalse();
    }

    @Test
    void parse_mapsSuccessAndFailure() {
        var ok = gateway.parse(IPN.formatted(IPN_SIGNATURE));
        assertThat(ok.orderId()).isEqualTo(ORDER);
        assertThat(ok.success()).isTrue();
        assertThat(ok.txnId()).isEqualTo("4088878653");
        assertThat(ok.amount()).isEqualByComparingTo("890000");

        var failed = gateway.parse(IPN.formatted(IPN_SIGNATURE).replace("\"resultCode\":0", "\"resultCode\":1006"));
        assertThat(failed.success()).isFalse();
        assertThat(failed.reason()).isEqualTo("momo-1006");
    }
}
