package vn.ecommerce.order.shipping;

import java.math.BigDecimal;

/** Một lựa chọn giao hàng cho khách; {@code fee = 0} khi được freeship. */
public record ShippingOption(String carrier, String carrierName, BigDecimal fee, int etaDays) {
}
