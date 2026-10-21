package vn.ecommerce.order.payment.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param mock  cổng giả: bí mật ký webhook và mẫu URL trang thanh toán giả ({@code {orderId}}, {@code {orderCode}} được thay)
 * @param vnpay VNPay (E6.1); chỉ bật khi {@code enabled=true} và có tmnCode/hashSecret (P-03)
 */
@ConfigurationProperties(prefix = "ecommerce.payment")
public record PaymentProperties(Mock mock, Vnpay vnpay) {

    public record Mock(String secret, String payUrlTemplate) {
    }

    /**
     * @param payUrl            trang thanh toán của VNPay (sandbox hoặc production)
     * @param returnUrlTemplate trang kết quả trên storefront, {@code {orderCode}} được thay
     * @param expireAfter       thời hạn giao dịch ở VNPay, khớp {@code ecommerce.orders.payment-timeout}
     */
    public record Vnpay(boolean enabled, String tmnCode, String hashSecret, String payUrl, String returnUrlTemplate, Duration expireAfter) {
    }
}
