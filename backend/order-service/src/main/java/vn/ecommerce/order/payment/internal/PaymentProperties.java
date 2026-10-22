package vn.ecommerce.order.payment.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param mock  cổng giả: bí mật ký webhook và mẫu URL trang thanh toán giả ({@code {orderId}}, {@code {orderCode}} được thay)
 * @param vnpay VNPay (E6.1); chỉ bật khi {@code enabled=true} và có tmnCode/hashSecret (P-03)
 * @param momo  MoMo (E6.2); chỉ bật khi {@code enabled=true} và có partnerCode/accessKey/secretKey (P-03)
 */
@ConfigurationProperties(prefix = "ecommerce.payment")
public record PaymentProperties(Mock mock, Vnpay vnpay, Momo momo) {

    public record Mock(String secret, String payUrlTemplate) {
    }

    /**
     * @param payUrl            trang thanh toán của VNPay (sandbox hoặc production)
     * @param returnUrlTemplate trang kết quả trên storefront, {@code {orderCode}} được thay
     * @param expireAfter       thời hạn giao dịch ở VNPay, khớp {@code ecommerce.orders.payment-timeout}
     */
    public record Vnpay(boolean enabled, String tmnCode, String hashSecret, String payUrl, String returnUrlTemplate, Duration expireAfter) {
    }

    /**
     * @param endpoint            gốc API MoMo (test-payment.momo.vn hoặc payment.momo.vn)
     * @param requestType         {@code captureWallet} (ví MoMo) hoặc {@code payWithMethod} (cho chọn ATM/thẻ)
     * @param redirectUrlTemplate trang kết quả trên storefront, {@code {orderCode}} được thay
     * @param ipnUrl              URL public của webhook {@code /webhooks/payments/momo} qua gateway
     */
    public record Momo(boolean enabled, String partnerCode, String accessKey, String secretKey, String endpoint, String requestType,
                       String redirectUrlTemplate, String ipnUrl) {
    }
}
