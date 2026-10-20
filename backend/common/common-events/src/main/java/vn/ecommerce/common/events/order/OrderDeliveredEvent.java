package vn.ecommerce.common.events.order;

import java.time.Instant;
import java.util.UUID;
import vn.ecommerce.common.events.IntegrationEvent;

/** Đơn đã giao thành công; notification-service mời đánh giá, catalog mở quyền review (giai đoạn sau). */
public record OrderDeliveredEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String orderCode,
        String customerId,
        String contactEmail) implements IntegrationEvent {
}
