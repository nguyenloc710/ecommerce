package vn.ecommerce.order.orders.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.order.orders.internal.report.RevenueReport;
import vn.ecommerce.order.orders.internal.report.RevenueReportService;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Báo cáo")
class RevenueReportController {

    private final RevenueReportService reportService;

    @GetMapping("/revenue")
    @Operation(summary = "Báo cáo doanh thu (chỉ ADMIN)", description = "Khoảng ngày [from, to] theo giờ Việt Nam, `groupBy` = DAY | MONTH. "
            + "Doanh thu tính đơn đã xác nhận/đã thanh toán và chưa trả hàng.")
    RevenueReport revenue(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(defaultValue = "DAY") String groupBy) {
        return reportService.revenue(from, to, groupBy);
    }
}
