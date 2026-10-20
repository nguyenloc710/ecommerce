package vn.ecommerce.common.jpa;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import vn.ecommerce.common.security.CurrentUser;

/**
 * Bật JPA Auditing cho mọi service dùng common-jpa. Service phải khai báo
 * {@code @EntityScan} gồm cả {@code vn.ecommerce.common.jpa} để Envers thấy {@link AuditRevisionEntity}.
 */
@AutoConfiguration
@EnableJpaAuditing(auditorAwareRef = "currentAuditorProvider")
public class JpaAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CurrentUser currentUser() {
        return new CurrentUser();
    }

    @Bean
    @ConditionalOnMissingBean(name = "currentAuditorProvider")
    CurrentAuditorProvider currentAuditorProvider(CurrentUser currentUser) {
        return new CurrentAuditorProvider(currentUser);
    }
}
