package vn.ecommerce.order.cart;

import java.util.Collection;
import java.util.UUID;

/** API public của module cart cho checkout: dọn các dòng đã đặt hàng khỏi giỏ server. */
public interface CartApi {

    void removeLines(UUID customerId, Collection<UUID> skuIds);
}
