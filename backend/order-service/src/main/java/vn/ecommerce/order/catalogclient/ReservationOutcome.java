package vn.ecommerce.order.catalogclient;

import java.time.Instant;
import java.util.UUID;

/** Kết quả giữ hàng; {@code expiresAt} = hạn thanh toán online (B-03, B-20). */
public record ReservationOutcome(UUID orderId, String status, Instant expiresAt) {
}
