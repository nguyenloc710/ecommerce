package vn.ecommerce.catalog.flashsale.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param keyGrace        khóa Valkey sống thêm sau giờ kết thúc để đơn đang chờ thanh toán vẫn trả suất được
 * @param maxItemsPerSale số SKU tối đa mỗi chương trình
 */
@ConfigurationProperties(prefix = "ecommerce.flash-sale")
public record FlashSaleProperties(Duration keyGrace, int maxItemsPerSale) {
}
