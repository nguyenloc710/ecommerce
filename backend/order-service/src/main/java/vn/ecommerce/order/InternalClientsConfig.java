package vn.ecommerce.order;

import org.springframework.beans.factory.annotation.Value;
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

/**
 * Interceptor gắn token client_credentials (role INTERNAL) cho mọi RestClient gọi {@code /internal/**} của service khác.
 * Token là của service nên cache theo một principal cố định, không theo khách đang gọi.
 */
@Configuration
class InternalClientsConfig {

    private static final Authentication SERVICE_PRINCIPAL = new AnonymousAuthenticationToken(
            "order-service", "order-service", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    @Bean
    OAuth2ClientHttpRequestInterceptor internalClientInterceptor(ClientRegistrationRepository registrations,
            @Value("${ecommerce.clients.internal-registration-id:keycloak-internal}") String registrationId) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations,
                new InMemoryOAuth2AuthorizedClientService(registrations));
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        interceptor.setClientRegistrationIdResolver(request -> registrationId);
        interceptor.setPrincipalResolver(request -> SERVICE_PRINCIPAL);
        return interceptor;
    }
}
