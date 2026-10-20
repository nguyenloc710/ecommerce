package vn.ecommerce.order.shipping.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShippingProperties.class)
class ShippingModuleConfig {
}
