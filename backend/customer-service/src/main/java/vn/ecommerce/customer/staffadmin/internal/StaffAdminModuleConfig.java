package vn.ecommerce.customer.staffadmin.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import vn.ecommerce.customer.staffadmin.internal.client.KeycloakAdminProperties;

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
class StaffAdminModuleConfig {

    /** Token client credentials là của service, không phải của admin đang thao tác, nên cache theo một principal cố định. */
    private static final Authentication SERVICE_PRINCIPAL = new AnonymousAuthenticationToken(
            "customer-service", "customer-service", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    @Bean
    RestClient keycloakAdminHttpClient(RestClient.Builder builder, ClientRegistrationRepository registrations,
                                       KeycloakAdminProperties properties) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations,
                new InMemoryOAuth2AuthorizedClientService(registrations));
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        interceptor.setClientRegistrationIdResolver(request -> properties.registrationId());
        interceptor.setPrincipalResolver(request -> SERVICE_PRINCIPAL);
        return builder.clone().baseUrl(properties.adminRealmUrl()).requestInterceptor(interceptor).build();
    }
}
