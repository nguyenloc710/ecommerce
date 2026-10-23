package vn.ecommerce.common.events.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.ecommerce.common.events.IntegrationEvent;

/**
 * Đơn đã giao thành công; notification-service mời đánh giá, catalog mở quyền review cho các SKU trong đơn (B-10).
 *
 * @param lines thêm ở E7.1 (chỉ thêm field, tương thích ngược): message cũ không có field này thì {@code null}
 */
public record OrderDeliveredEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String orderCode,
        String customerId,
        String contactEmail,
        List<Line> lines) implements IntegrationEvent {

    public record Line(UUID skuId, String productName) {
    }
}
