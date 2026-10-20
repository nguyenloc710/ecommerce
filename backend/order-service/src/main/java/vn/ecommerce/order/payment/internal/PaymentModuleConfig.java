package vn.ecommerce.order.payment.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
class PaymentModuleConfig {
}
