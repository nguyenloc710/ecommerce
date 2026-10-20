package vn.ecommerce.catalog.inventory.internal.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Trạng thái giữ hàng của một đơn (tổng hợp theo dòng). */
public record ReservationResult(UUID orderId, String status, Instant expiresAt, List<Line> lines) {

    public record Line(UUID skuId, int quantity, String status) {
    }
}
