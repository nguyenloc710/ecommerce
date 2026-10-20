package vn.ecommerce.common.events.order;

import java.time.Instant;
import java.util.UUID;
import vn.ecommerce.common.events.IntegrationEvent;

/** Đơn đã bàn giao cho hãng vận chuyển; notification-service gửi mã vận đơn cho khách. */
public record OrderShippedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String orderCode,
        String customerId,
        String contactEmail,
        String carrier,
        String trackingCode) implements IntegrationEvent {
}
