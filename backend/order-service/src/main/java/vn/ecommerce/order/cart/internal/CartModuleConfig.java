package vn.ecommerce.order.cart.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CartProperties.class)
class CartModuleConfig {
}
