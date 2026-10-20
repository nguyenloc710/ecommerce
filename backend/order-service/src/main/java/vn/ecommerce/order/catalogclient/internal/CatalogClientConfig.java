package vn.ecommerce.order.catalogclient.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CatalogClientProperties.class)
class CatalogClientConfig {

    @Bean
    RestClient catalogHttpClient(RestClient.Builder builder, OAuth2ClientHttpRequestInterceptor internalClientInterceptor,
                                 CatalogClientProperties properties) {
        return builder.clone().baseUrl(properties.baseUrl()).requestInterceptor(internalClientInterceptor).build();
    }
}
