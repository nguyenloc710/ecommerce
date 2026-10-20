package vn.ecommerce.catalog.inventory.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình module inventory.
 *
 * @param reservationTtl        thời gian giữ hàng cho đơn chưa thanh toán (B-03: 15 phút)
 * @param optimisticRetries     số lần thử lại khi đụng optimistic lock lúc giữ/nhả hàng
 * @param defaultLowStock       ngưỡng "sắp hết" mặc định khi tạo inventory_item
 * @param expiryBatchSize       số đơn hết hạn xử lý mỗi lần job chạy
 */
@ConfigurationProperties(prefix = "ecommerce.inventory")
public record InventoryProperties(Duration reservationTtl, Integer optimisticRetries, Integer defaultLowStock, Integer expiryBatchSize) {

    public InventoryProperties {
        reservationTtl = reservationTtl == null ? Duration.ofMinutes(15) : reservationTtl;
        optimisticRetries = optimisticRetries == null ? 3 : optimisticRetries;
        defaultLowStock = defaultLowStock == null ? 3 : defaultLowStock;
        expiryBatchSize = expiryBatchSize == null ? 200 : expiryBatchSize;
    }
}
