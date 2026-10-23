package vn.ecommerce.order.orders.internal.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.order.orders.OrderStatus;
import vn.ecommerce.order.orders.internal.OrderProperties;
import vn.ecommerce.order.orders.internal.report.RevenueReport.MethodRow;
import vn.ecommerce.order.orders.internal.report.RevenueReport.Point;
import vn.ecommerce.order.orders.internal.report.RevenueReport.Summary;
import vn.ecommerce.order.orders.internal.report.RevenueReport.TopProduct;
import vn.ecommerce.order.orders.internal.report.RevenueReportRepository.PeriodTotals;
import vn.ecommerce.order.orders.internal.report.RevenueReportRepository.StatusTotals;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class RevenueReportServiceImpl implements RevenueReportService {

    private static final String MONTH = "MONTH";
    private static final String DAY = "DAY";

    private final RevenueReportRepository repository;
    private final OrderProperties orderProperties;
    private final ReportProperties properties;

    @Override
    public RevenueReport revenue(LocalDate from, LocalDate to, String groupBy) {
        validateRange(from, to);
        String unit = MONTH.equalsIgnoreCase(groupBy) ? MONTH : DAY;
        ZoneId zone = orderProperties.zoneId();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<String> counted = RevenueStatuses.COUNTED_NAMES;

        Summary summary = summarize(repository.totalsByStatus(start, end));
        List<Point> series = fillSeries(from, to, unit, repository.totalsByPeriod(unit.toLowerCase(), zone.getId(), counted, start, end));
        List<TopProduct> top = repository.topProducts(counted, start, end, properties.topProducts()).stream()
                .map(p -> new TopProduct(p.getProductId(), p.getProductName(), p.getProductSlug(), p.getQuantity(), p.getRevenue())).toList();
        List<MethodRow> methods = repository.totalsByPaymentMethod(counted, start, end).stream()
                .map(m -> new MethodRow(m.getMethod(), m.getOrderCount(), m.getNetRevenue())).toList();
        return new RevenueReport(from, to, unit, summary, series, top, methods);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) >= properties.maxRangeDays()) {
            throw new BadRequestException(ReportErrorKeys.INVALID_RANGE, properties.maxRangeDays());
        }
    }

    private static Summary summarize(List<StatusTotals> rows) {
        List<StatusTotals> counted = byStatus(rows, RevenueStatuses.COUNTED);
        List<StatusTotals> returned = byStatus(rows, RevenueStatuses.RETURNED);
        long orderCount = counted.stream().mapToLong(StatusTotals::getOrderCount).sum();
        BigDecimal net = sum(counted, StatusTotals::getTotal);
        BigDecimal average = orderCount == 0 ? BigDecimal.ZERO : net.divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP);
        return new Summary(orderCount, sum(counted, StatusTotals::getSubtotal), sum(counted, StatusTotals::getDiscount),
                sum(counted, StatusTotals::getShippingFee), net, average,
                byStatus(rows, RevenueStatuses.AWAITING).stream().mapToLong(StatusTotals::getOrderCount).sum(),
                byStatus(rows, Set.of(OrderStatus.CANCELLED)).stream().mapToLong(StatusTotals::getOrderCount).sum(),
                returned.stream().mapToLong(StatusTotals::getOrderCount).sum(), sum(returned, StatusTotals::getTotal));
    }

    private static List<StatusTotals> byStatus(List<StatusTotals> rows, Set<OrderStatus> statuses) {
        return rows.stream().filter(r -> statuses.stream().anyMatch(s -> s.name().equals(r.getStatus()))).toList();
    }

    private static BigDecimal sum(List<StatusTotals> rows, Function<StatusTotals, BigDecimal> field) {
        return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Mọi kỳ trong khoảng đều có điểm (kỳ không có đơn = 0) để biểu đồ không bị đứt. */
    private static List<Point> fillSeries(LocalDate from, LocalDate to, String unit, List<PeriodTotals> rows) {
        Map<LocalDate, PeriodTotals> byPeriod = rows.stream().collect(Collectors.toMap(r -> LocalDate.parse(r.getPeriod()), Function.identity()));
        List<Point> points = new ArrayList<>();
        boolean monthly = MONTH.equals(unit);
        for (LocalDate d = monthly ? from.withDayOfMonth(1) : from; !d.isAfter(to); d = monthly ? d.plusMonths(1) : d.plusDays(1)) {
            PeriodTotals row = byPeriod.get(d);
            points.add(row == null ? new Point(d, 0, BigDecimal.ZERO) : new Point(d, row.getOrderCount(), row.getNetRevenue()));
        }
        return points;
    }
}
