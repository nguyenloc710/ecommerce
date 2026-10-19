package vn.ecommerce.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

class ResourceServerAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class,
                    OAuth2ResourceServerAutoConfiguration.class,
                    ResourceServerAutoConfiguration.class))
            .withPropertyValues(
                    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs");

    @Test
    void registersOnlyCommonChain_insteadOfBootDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBeans(SecurityFilterChain.class)
                    .containsOnlyKeys("resourceServerFilterChain");
            assertThat(context).hasSingleBean(CurrentUser.class);
        });
    }
}
