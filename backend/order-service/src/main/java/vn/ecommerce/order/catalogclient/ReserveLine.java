package vn.ecommerce.order.catalogclient;

import java.util.UUID;

public record ReserveLine(UUID skuId, int quantity) {
}
