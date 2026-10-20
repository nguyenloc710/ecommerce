package vn.ecommerce.common.jpa;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import vn.ecommerce.common.security.CurrentUser;

/** Người thực hiện thao tác = {@code sub} của Keycloak; job, consumer, migration ghi {@value #SYSTEM}. */
@RequiredArgsConstructor
public class CurrentAuditorProvider implements AuditorAware<String> {

    public static final String SYSTEM = "system";

    private final CurrentUser currentUser;

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(currentUser.id().orElse(SYSTEM));
    }
}
