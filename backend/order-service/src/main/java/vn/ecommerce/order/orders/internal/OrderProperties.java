package vn.ecommerce.order.orders.internal;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param paymentTimeout      đơn online chưa thanh toán quá hạn này sẽ bị hủy (B-20)
 * @param expiryCheckInterval chu kỳ job kiểm tra
 * @param zone                múi giờ để đánh số mã đơn theo ngày (B-02)
 */
@ConfigurationProperties(prefix = "ecommerce.orders")
public record OrderProperties(Duration paymentTimeout, Duration expiryCheckInterval, String zone) {

    public OrderProperties {
        paymentTimeout = paymentTimeout == null ? Duration.ofMinutes(15) : paymentTimeout;
        expiryCheckInterval = expiryCheckInterval == null ? Duration.ofSeconds(60) : expiryCheckInterval;
        zone = zone == null || zone.isBlank() ? "Asia/Ho_Chi_Minh" : zone;
    }

    public ZoneId zoneId() {
        return ZoneId.of(zone);
    }
}
