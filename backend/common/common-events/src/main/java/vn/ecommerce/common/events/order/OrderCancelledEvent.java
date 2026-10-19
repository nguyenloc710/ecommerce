package vn.ecommerce.common.events.order;

import java.time.Instant;
import java.util.UUID;
import vn.ecommerce.common.events.IntegrationEvent;

/** Đơn bị hủy (khách hủy, thanh toán thất bại hoặc hết hạn). catalog-service nhả reservation. */
public record OrderCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String orderCode,
        String customerId,
        String contactEmail,
        String reason) implements IntegrationEvent {
}
