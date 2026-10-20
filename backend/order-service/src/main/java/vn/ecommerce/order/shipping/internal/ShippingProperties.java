package vn.ecommerce.order.shipping.internal;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param carriers          hãng đang bật và phí mặc định khi chưa có API thật (B-07)
 * @param freeshipThreshold tạm tính từ mức này trở lên được miễn phí giao hàng
 * @param mock              bí mật ký webhook của hãng giả
 */
@ConfigurationProperties(prefix = "ecommerce.shipping")
public record ShippingProperties(List<CarrierConfig> carriers, BigDecimal freeshipThreshold, Mock mock) {

    public ShippingProperties {
        carriers = carriers == null ? List.of() : List.copyOf(carriers);
        freeshipThreshold = freeshipThreshold == null ? BigDecimal.valueOf(500_000) : freeshipThreshold;
    }

    public record CarrierConfig(String code, String name, BigDecimal fee, Integer etaDays) {
        public CarrierConfig {
            etaDays = etaDays == null ? 3 : etaDays;
        }
    }

    public record Mock(String secret) {
    }
}
