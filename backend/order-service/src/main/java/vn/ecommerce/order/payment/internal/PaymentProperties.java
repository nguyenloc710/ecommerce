package vn.ecommerce.order.payment.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param mock cổng giả: bí mật ký webhook và mẫu URL trang thanh toán giả ({@code {orderId}} được thay) */
@ConfigurationProperties(prefix = "ecommerce.payment")
public record PaymentProperties(Mock mock) {

    public record Mock(String secret, String payUrlTemplate) {
    }
}
