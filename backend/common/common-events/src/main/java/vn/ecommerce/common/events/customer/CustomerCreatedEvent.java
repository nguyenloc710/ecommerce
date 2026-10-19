package vn.ecommerce.common.events.customer;

import java.time.Instant;
import java.util.UUID;
import vn.ecommerce.common.events.IntegrationEvent;

/**
 * Hồ sơ khách hàng vừa được tạo (lần đăng nhập đầu tiên).
 *
 * @param customerId {@code sub} của Keycloak
 */
public record CustomerCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String customerId,
        String email,
        String fullName) implements IntegrationEvent {
}
