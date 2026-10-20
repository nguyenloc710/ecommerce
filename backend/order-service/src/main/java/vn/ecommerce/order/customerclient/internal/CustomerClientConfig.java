package vn.ecommerce.order.customerclient.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CustomerClientConfig.CustomerClientProperties.class)
class CustomerClientConfig {

    /** @param baseUrl gốc customer-service (K8s: {@code http://customer-service:8080}) */
    @ConfigurationProperties(prefix = "ecommerce.clients.customer")
    record CustomerClientProperties(String baseUrl) {
    }

    @Bean
    RestClient customerHttpClient(RestClient.Builder builder, OAuth2ClientHttpRequestInterceptor internalClientInterceptor,
                                  CustomerClientProperties properties) {
        return builder.clone().baseUrl(properties.baseUrl()).requestInterceptor(internalClientInterceptor).build();
    }
}
