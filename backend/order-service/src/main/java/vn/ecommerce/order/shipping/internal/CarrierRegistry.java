package vn.ecommerce.order.shipping.internal;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import vn.ecommerce.order.shipping.ShippingOption;

/** Gom các hãng theo mã; bean thật (nếu có) thay cho hãng giả cùng mã trong cấu hình. */
@Component
class CarrierRegistry {

    private final Map<String, ShippingCarrier> carriers = new LinkedHashMap<>();
    private final BigDecimal freeshipThreshold;

    CarrierRegistry(ShippingProperties properties, List<ShippingCarrier> realCarriers) {
        properties.carriers().forEach(config -> {
            MockCarrier mock = MockCarrier.from(config);
            carriers.put(mock.code(), mock);
        });
        realCarriers.forEach(carrier -> carriers.put(carrier.code().toUpperCase(), carrier));
        this.freeshipThreshold = properties.freeshipThreshold();
    }

    Optional<ShippingCarrier> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(carriers.get(code.trim().toUpperCase()));
    }

    List<ShippingCarrier> all() {
        return List.copyOf(carriers.values());
    }

    ShippingOption optionFor(ShippingCarrier carrier, BigDecimal subtotal, String provinceCode) {
        BigDecimal fee = subtotal.compareTo(freeshipThreshold) >= 0 ? BigDecimal.ZERO : carrier.quote(subtotal, provinceCode);
        return new ShippingOption(carrier.code(), carrier.name(), fee.setScale(2, java.math.RoundingMode.HALF_UP), carrier.etaDays());
    }
}
