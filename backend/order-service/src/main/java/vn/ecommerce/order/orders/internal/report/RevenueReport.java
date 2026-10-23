package vn.ecommerce.order.orders.internal.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Báo cáo doanh thu trong khoảng ngày [from, to] (giờ Việt Nam). Doanh thu chỉ tính đơn đã xác nhận trở đi và chưa
 * trả hàng ({@link RevenueStatuses#COUNTED}); đơn chờ thanh toán, đã hủy, đã trả/hoàn tiền tách riêng.
 *
 * @param groupBy DAY | MONTH
 */
public record RevenueReport(LocalDate from, LocalDate to, String groupBy, Summary summary, List<Point> series, List<TopProduct> topProducts,
                            List<MethodRow> byPaymentMethod) {

    /**
     * @param grossSales        tổng tiền hàng trước giảm giá
     * @param netRevenue        tổng thực thu (sau giảm giá, gồm phí ship)
     * @param averageOrderValue {@code netRevenue / orderCount}, làm tròn tới đồng
     * @param returnedAmount    tổng tiền các đơn đã trả hàng / hoàn tiền
     */
    public record Summary(long orderCount, BigDecimal grossSales, BigDecimal discount, BigDecimal shippingFee, BigDecimal netRevenue,
                          BigDecimal averageOrderValue, long awaitingPaymentCount, long cancelledCount, long returnedCount,
                          BigDecimal returnedAmount) {
    }

    /** @param period ngày đầu kỳ (ngày, hoặc ngày 1 của tháng) */
    public record Point(LocalDate period, long orderCount, BigDecimal netRevenue) {
    }

    /** @param revenue tổng tiền dòng hàng (trước giảm giá đơn) */
    public record TopProduct(UUID productId, String productName, String productSlug, long quantity, BigDecimal revenue) {
    }

    public record MethodRow(String method, long orderCount, BigDecimal netRevenue) {
    }
}
