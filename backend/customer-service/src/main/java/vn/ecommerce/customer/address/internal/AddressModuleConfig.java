package vn.ecommerce.customer.address.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AddressProperties.class)
class AddressModuleConfig {
}
