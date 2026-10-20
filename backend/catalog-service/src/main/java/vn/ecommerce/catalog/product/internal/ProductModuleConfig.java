package vn.ecommerce.catalog.product.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProductProperties.class)
class ProductModuleConfig {
}
