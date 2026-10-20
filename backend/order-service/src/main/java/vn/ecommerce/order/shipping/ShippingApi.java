package vn.ecommerce.order.shipping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** API public của module shipping cho checkout (báo giá) và orders (tạo vận đơn). */
public interface ShippingApi {

    /** Mọi hãng đang bật kèm phí cho tạm tính/tỉnh này (đã áp freeship B-07). */
    List<ShippingOption> options(BigDecimal subtotal, String provinceCode);

    /** @throws vn.ecommerce.common.web.error.BadRequestException {@code shipping.carrier-not-found} */
    ShippingOption option(String carrierCode, BigDecimal subtotal, String provinceCode);

    /** @throws vn.ecommerce.common.web.error.ConflictException {@code shipping.shipment-exists} nếu đơn đã có vận đơn */
    ShipmentView createShipment(UUID orderId, String carrierCode, BigDecimal fee);

    Optional<ShipmentView> findByOrder(UUID orderId);
}
