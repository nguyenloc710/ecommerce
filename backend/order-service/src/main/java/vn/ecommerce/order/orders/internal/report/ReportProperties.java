package vn.ecommerce.order.orders.internal.report;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param maxRangeDays số ngày tối đa mỗi lần xem báo cáo
 * @param topProducts  số sản phẩm bán chạy trả về
 */
@ConfigurationProperties(prefix = "ecommerce.reports")
public record ReportProperties(int maxRangeDays, int topProducts) {

    public ReportProperties {
        maxRangeDays = maxRangeDays <= 0 ? 366 : maxRangeDays;
        topProducts = topProducts <= 0 ? 10 : topProducts;
    }
}
