package vn.ecommerce.common.events.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.ecommerce.common.events.IntegrationEvent;

/**
 * Đơn đã xác nhận (COD hoặc đã thanh toán). catalog-service chuyển reservation sang COMMITTED,
 * notification-service gửi email xác nhận.
 */
public record OrderConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String orderCode,
        String customerId,
        String contactEmail,
        BigDecimal total,
        List<Line> lines) implements IntegrationEvent {

    public record Line(UUID skuId, String productName, String variantLabel, int quantity, BigDecimal unitPrice) {
    }
}
