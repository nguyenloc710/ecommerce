package vn.ecommerce.order.shipping;

import java.util.UUID;

/**
 * Event nội bộ (Spring) khi hãng vận chuyển báo trạng thái mới qua webhook; module orders nghe để chuyển
 * SHIPPING → DELIVERED. Dùng event thay vì gọi thẳng để shipping không phụ thuộc ngược vào orders.
 *
 * @param status CREATED | IN_TRANSIT | DELIVERED | FAILED
 */
public record ShipmentStatusChanged(UUID orderId, String carrier, String trackingCode, String status) {

    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";
}
