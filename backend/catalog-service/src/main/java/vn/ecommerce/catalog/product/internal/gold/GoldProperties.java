package vn.ecommerce.catalog.product.internal.gold;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param priceRounding giá tính theo vàng làm tròn (nửa lên) tới bội số này, ví dụ 1000 đ */
@ConfigurationProperties(prefix = "ecommerce.gold")
public record GoldProperties(BigDecimal priceRounding) {

    public GoldProperties {
        priceRounding = priceRounding == null || priceRounding.signum() <= 0 ? BigDecimal.valueOf(1000) : priceRounding;
    }
}
