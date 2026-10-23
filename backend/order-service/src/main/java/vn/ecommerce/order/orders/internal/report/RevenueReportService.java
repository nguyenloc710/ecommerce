package vn.ecommerce.order.orders.internal.report;

import java.time.LocalDate;

/** Báo cáo doanh thu cho chủ shop (ADMIN, B-11). */
public interface RevenueReportService {

    /** @param groupBy DAY (mặc định) | MONTH; kỳ không có đơn vẫn có điểm với giá trị 0 */
    RevenueReport revenue(LocalDate from, LocalDate to, String groupBy);
}
