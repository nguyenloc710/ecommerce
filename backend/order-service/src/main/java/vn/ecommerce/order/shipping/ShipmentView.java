package vn.ecommerce.order.shipping;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentView(UUID id, UUID orderId, String carrier, String trackingCode, BigDecimal fee, String status) {
}
