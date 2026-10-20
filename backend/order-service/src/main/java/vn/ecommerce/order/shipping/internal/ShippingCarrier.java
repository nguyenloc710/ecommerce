package vn.ecommerce.order.shipping.internal;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Strategy cho từng hãng vận chuyển. Thêm hãng thật (GHN/GHTK API) = thêm một bean cùng {@code code};
 * {@link CarrierRegistry} ưu tiên bean thật hơn {@link MockCarrier} sinh từ cấu hình.
 */
public interface ShippingCarrier {

    String code();

    String name();

    int etaDays();

    /** Phí trước khi áp freeship (freeship do {@link CarrierRegistry} quyết định theo cấu hình chung). */
    BigDecimal quote(BigDecimal subtotal, String provinceCode);

    /** Tạo vận đơn ở hãng và trả mã theo dõi. */
    String createTracking(UUID orderId);
}
