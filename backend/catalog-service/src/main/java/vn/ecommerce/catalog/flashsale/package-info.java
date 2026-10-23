/**
 * Module flashsale (E7.2): chương trình giảm giá có giờ bắt đầu/kết thúc và số suất. Áp giá qua SPI
 * {@code product.PriceAdjuster}, giữ suất khi giữ hàng qua SPI {@code inventory.ReservationGuard} (Lua trên Valkey).
 */
@ApplicationModule(displayName = "flashsale")
package vn.ecommerce.catalog.flashsale;

import org.springframework.modulith.ApplicationModule;
