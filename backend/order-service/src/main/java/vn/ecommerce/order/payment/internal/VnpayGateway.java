package vn.ecommerce.order.payment.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.PaymentRequest;

/**
 * Cổng VNPay 2.1.0 (E6.1). Khách được chuyển sang trang VNPay theo payUrl; kết quả chính thức đến qua IPN
 * ({@code GET /webhooks/payments/vnpay}), còn Return URL chỉ để storefront hiện trạng thái đơn.
 *
 * <p>Chỉ có bean khi {@code ecommerce.payment.vnpay.enabled=true} (P-03): chưa có tmnCode/hashSecret thì checkout
 * không cho chọn VNPay. {@code vnp_TxnRef} = orderId bỏ dấu gạch (mỗi đơn một giao dịch), nên IPN tra lại được đơn.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ecommerce.payment.vnpay", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
class VnpayGateway implements PaymentGateway {

    static final String VERSION = "2.1.0";
    static final String SUCCESS = "00";
    /** VNPay dùng giờ Việt Nam cho vnp_CreateDate/vnp_ExpireDate. */
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String FALLBACK_IP = "127.0.0.1";

    private final PaymentProperties properties;
    private final Clock clock;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public GatewayInit initiate(PaymentRequest request) {
        PaymentProperties.Vnpay cfg = properties.vnpay();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(VN);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", cfg.tmnCode());
        params.put("vnp_Amount", toVnpAmount(request.amount()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef(request.orderId()));
        // VNPay yêu cầu nội dung không dấu
        params.put("vnp_OrderInfo", "Thanh toan don hang " + request.orderCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", cfg.returnUrlTemplate().replace("{orderCode}", request.orderCode()));
        params.put("vnp_IpAddr", request.clientIp() == null || request.clientIp().isBlank() ? FALLBACK_IP : request.clientIp());
        params.put("vnp_CreateDate", VNP_TIME.format(now));
        params.put("vnp_ExpireDate", VNP_TIME.format(now.plus(cfg.expireAfter())));

        String query = VnpaySigner.canonical(params);
        String url = cfg.payUrl() + "?" + query + "&" + VnpaySigner.SECURE_HASH + "=" + VnpaySigner.sign(cfg.hashSecret(), query);
        return new GatewayInit(url, Payment.PaymentStatus.PENDING);
    }

    /** {@code rawBody} là query string IPN; chữ ký nằm trong chính tham số {@code vnp_SecureHash}. */
    @Override
    public boolean verify(String signature, String rawBody) {
        Map<String, String> params = VnpaySigner.parseQuery(rawBody);
        return signature != null && signature.equalsIgnoreCase(params.get(VnpaySigner.SECURE_HASH))
                && VnpaySigner.verify(properties.vnpay().hashSecret(), params);
    }

    @Override
    public WebhookNotification parse(String rawBody) {
        Map<String, String> p = VnpaySigner.parseQuery(rawBody);
        String responseCode = p.get("vnp_ResponseCode");
        boolean success = SUCCESS.equals(responseCode) && SUCCESS.equals(p.get("vnp_TransactionStatus"));
        // vnp_TransactionNo là "0" khi giao dịch hỏng: ghép TxnRef để mã idempotency không trùng giữa các đơn
        String txnId = p.get("vnp_TxnRef") + "-" + p.getOrDefault("vnp_TransactionNo", "0");
        BigDecimal amount = new BigDecimal(p.getOrDefault("vnp_Amount", "0")).movePointLeft(2);
        return new WebhookNotification(orderId(p.get("vnp_TxnRef")), txnId, success, amount, success ? null : "vnpay-" + responseCode);
    }

    /** VNPay nhận số tiền × 100, không phần thập phân. */
    static String toVnpAmount(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toPlainString();
    }

    static String txnRef(UUID orderId) {
        return orderId.toString().replace("-", "");
    }

    static UUID orderId(String txnRef) {
        if (txnRef == null || txnRef.length() != 32) {
            return null;
        }
        return UUID.fromString(txnRef.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5"));
    }
}
