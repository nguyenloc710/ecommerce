package vn.ecommerce.order.payment.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.PaymentRequest;

/**
 * Chữ ký VNPay đối chiếu với HMAC-SHA512 tính độc lập (Node crypto) trên chuỗi chuẩn hóa, để bắt lỗi sắp xếp/mã hóa
 * mà test tự-ký-tự-kiểm không thấy được.
 */
class VnpayGatewayTest {

    private static final String SECRET = "SECRETKEY123";
    private static final UUID ORDER = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final String TXN_REF = "0f8fad5bd9cb469fa16570867728950e";
    /** 08:30 UTC = 15:30 giờ Việt Nam. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T08:30:00Z"), ZoneOffset.UTC);

    private static final String EXPECTED_QUERY = "vnp_Amount=47500000&vnp_Command=pay&vnp_CreateDate=20261021153000&vnp_CurrCode=VND"
            + "&vnp_ExpireDate=20261021154500&vnp_IpAddr=203.0.113.7&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+ORD-261021-0007"
            + "&vnp_OrderType=other&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A3000%2Fthanh-toan%2Fket-qua%2FORD-261021-0007"
            + "&vnp_TmnCode=DEMO1234&vnp_TxnRef=" + TXN_REF + "&vnp_Version=2.1.0";
    private static final String EXPECTED_HASH =
            "6311e2c2056ae40b7fd58700583e5aee5d684bbb600c5f201f3d4fc113a49045b63e6cc8c2068950aa4038091629d1c07ee3773d3c1f055884a7623495f0c43d";

    private static final String IPN_SIGNED = "vnp_Amount=47500000&vnp_BankCode=NCB&vnp_OrderInfo=Thanh+toan+don+hang+ORD-261021-0007"
            + "&vnp_ResponseCode=00&vnp_TmnCode=DEMO1234&vnp_TransactionNo=14567890&vnp_TransactionStatus=00&vnp_TxnRef=" + TXN_REF;
    private static final String IPN_HASH =
            "46621c68818f69871d25c59cfd6ea9228479c2f872a0e1c5d8ff9f197859a71910a572631b57a548060b453d62b0eff394b425b59dec30600028cd65331fc9c8";

    private final VnpayGateway gateway = new VnpayGateway(new PaymentProperties(null, new PaymentProperties.Vnpay(true, "DEMO1234", SECRET,
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "http://localhost:3000/thanh-toan/ket-qua/{orderCode}", Duration.ofMinutes(15)), null),
            CLOCK);

    @Test
    void initiate_buildsSortedQuery_amountTimes100_vietnamTime_andHmacSha512() {
        var init = gateway.initiate(new PaymentRequest(ORDER, "ORD-261021-0007", new BigDecimal("475000.00"), PaymentMethod.VNPAY, "203.0.113.7"));

        assertThat(init.payUrl()).isEqualTo("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?" + EXPECTED_QUERY + "&vnp_SecureHash=" + EXPECTED_HASH);
        assertThat(init.initialStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
    }

    @Test
    void initiate_withoutClientIp_usesFallback() {
        var init = gateway.initiate(new PaymentRequest(ORDER, "ORD-1", new BigDecimal("100000"), PaymentMethod.VNPAY, null));

        assertThat(init.payUrl()).contains("vnp_IpAddr=127.0.0.1");
    }

    @Test
    void verify_acceptsValidIpn_evenWithHashTypeAndUppercaseHash() {
        String query = IPN_SIGNED + "&vnp_SecureHashType=HmacSHA512&vnp_SecureHash=" + IPN_HASH.toUpperCase();

        assertThat(gateway.verify(IPN_HASH.toUpperCase(), query)).isTrue();
    }

    @Test
    void verify_rejectsTamperedAmount() {
        String query = IPN_SIGNED.replace("vnp_Amount=47500000", "vnp_Amount=100") + "&vnp_SecureHash=" + IPN_HASH;

        assertThat(gateway.verify(IPN_HASH, query)).isFalse();
    }

    @Test
    void verify_rejectsMissingSignature() {
        assertThat(gateway.verify(null, IPN_SIGNED)).isFalse();
    }

    @Test
    void parse_successIpn_mapsOrderIdAmountAndUniqueTxnId() {
        var note = gateway.parse(IPN_SIGNED + "&vnp_SecureHash=" + IPN_HASH);

        assertThat(note.orderId()).isEqualTo(ORDER);
        assertThat(note.success()).isTrue();
        assertThat(note.amount()).isEqualByComparingTo("475000");
        assertThat(note.txnId()).isEqualTo(TXN_REF + "-14567890");
        assertThat(note.reason()).isNull();
    }

    @Test
    void parse_failedIpn_carriesResponseCodeAsReason() {
        var note = gateway.parse(IPN_SIGNED.replace("vnp_ResponseCode=00", "vnp_ResponseCode=24").replace("vnp_TransactionStatus=00", "vnp_TransactionStatus=02")
                .replace("vnp_TransactionNo=14567890", "vnp_TransactionNo=0"));

        assertThat(note.success()).isFalse();
        assertThat(note.reason()).isEqualTo("vnpay-24");
        // Giao dịch hỏng có TransactionNo = 0: vẫn phải khác nhau giữa các đơn để không bị coi là "đã xử lý"
        assertThat(note.txnId()).isEqualTo(TXN_REF + "-0");
    }
}
