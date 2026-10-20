package vn.ecommerce.order.shipping.internal;

import java.math.BigDecimal;
import java.util.UUID;

/** Hãng giả theo cấu hình: phí cố định, mã vận đơn {@code MOCK-<CODE>-xxxxxxxx}. */
record MockCarrier(String code, String name, BigDecimal fee, int etaDays) implements ShippingCarrier {

    static MockCarrier from(ShippingProperties.CarrierConfig config) {
        return new MockCarrier(config.code().toUpperCase(), config.name(), config.fee(), config.etaDays());
    }

    @Override
    public BigDecimal quote(BigDecimal subtotal, String provinceCode) {
        return fee;
    }

    @Override
    public String createTracking(UUID orderId) {
        return "MOCK-" + code + "-" + orderId.toString().substring(0, 8).toUpperCase();
    }
}
