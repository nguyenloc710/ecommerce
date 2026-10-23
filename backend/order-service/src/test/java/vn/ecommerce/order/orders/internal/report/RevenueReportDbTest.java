package vn.ecommerce.order.orders.internal.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.order.orders.internal.OrderProperties;

/**
 * Báo cáo chạy trên PostgreSQL thật: native SQL gom theo ngày giờ Việt Nam ({@code date_trunc ... AT TIME ZONE}),
 * nhóm trạng thái tính doanh thu, sản phẩm bán chạy, phương thức thanh toán. Đơn đặt 23:30 giờ VN rơi vào đúng ngày VN
 * dù theo UTC là ngày khác.
 */
/*
 * @DirtiesContext: đóng context ngay sau class, khi container PostgreSQL còn sống (E0.7).
 */
@Testcontainers
@DirtiesContext
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RevenueReportServiceImpl.class, RevenueReportDbTest.Props.class})
class RevenueReportDbTest {

    private static final UUID RING = UUID.fromString("40000000-0000-0000-0000-000000000702");
    private static final UUID NECKLACE = UUID.fromString("40000000-0000-0000-0000-000000000801");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @TestConfiguration
    @EnableConfigurationProperties({OrderProperties.class, ReportProperties.class})
    static class Props {
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RevenueReportService reports;

    @BeforeEach
    void seed() {
        UUID o1 = order("CONFIRMED", "COD", "2026-10-01T10:00+07:00", "750000", "40000", "0", "710000");
        item(o1, RING, "Nhẫn moissanite", 1, "450000");
        item(o1, NECKLACE, "Dây chuyền", 1, "300000");
        UUID o2 = order("DELIVERED", "VNPAY", "2026-10-02T23:30+07:00", "300000", "0", "25000", "325000");
        item(o2, NECKLACE, "Dây chuyền", 1, "300000");
        order("CANCELLED", "COD", "2026-10-02T09:00+07:00", "450000", "0", "0", "450000");
        order("AWAITING_PAYMENT", "MOMO", "2026-10-03T08:00+07:00", "300000", "0", "25000", "325000");
        order("REFUNDED", "VNPAY", "2026-10-03T12:00+07:00", "200000", "0", "0", "200000");
        // Ngoài khoảng theo giờ VN (dù theo UTC thì 30/09 23:30 VN vẫn là 30/09, 04/10 00:30 VN là 03/10)
        order("COMPLETED", "COD", "2026-09-30T23:30+07:00", "999000", "0", "0", "999000");
        order("COMPLETED", "COD", "2026-10-04T00:30+07:00", "888000", "0", "0", "888000");
    }

    @Test
    void daily_countsOnlyRevenueStatuses_inVietnamDays_andFillsEmptyDays() {
        RevenueReport report = reports.revenue(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03"), "DAY");

        RevenueReport.Summary s = report.summary();
        assertThat(s.orderCount()).isEqualTo(2);
        assertThat(s.grossSales()).isEqualByComparingTo("1050000");
        assertThat(s.discount()).isEqualByComparingTo("40000");
        assertThat(s.shippingFee()).isEqualByComparingTo("25000");
        assertThat(s.netRevenue()).isEqualByComparingTo("1035000");
        assertThat(s.averageOrderValue()).isEqualByComparingTo("517500");
        assertThat(s.awaitingPaymentCount()).isEqualTo(1);
        assertThat(s.cancelledCount()).isEqualTo(1);
        assertThat(s.returnedCount()).isEqualTo(1);
        assertThat(s.returnedAmount()).isEqualByComparingTo("200000");

        assertThat(report.series()).extracting(RevenueReport.Point::period)
                .containsExactly(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-03"));
        assertThat(report.series()).extracting(p -> p.netRevenue().intValue()).containsExactly(710000, 325000, 0);

        assertThat(report.topProducts()).extracting(RevenueReport.TopProduct::productId).containsExactly(NECKLACE, RING);
        assertThat(report.topProducts().getFirst().quantity()).isEqualTo(2);
        assertThat(report.topProducts().getFirst().revenue()).isEqualByComparingTo("600000");

        assertThat(report.byPaymentMethod()).extracting(RevenueReport.MethodRow::method).containsExactly("COD", "VNPAY");
    }

    @Test
    void monthly_groupsIntoFirstDayOfMonth() {
        RevenueReport report = reports.revenue(LocalDate.parse("2026-09-15"), LocalDate.parse("2026-10-03"), "month");

        assertThat(report.groupBy()).isEqualTo("MONTH");
        assertThat(report.series()).extracting(RevenueReport.Point::period)
                .containsExactly(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));
        assertThat(report.series()).extracting(p -> p.netRevenue().intValue()).containsExactly(999000, 1035000);
    }

    @Test
    void rejectsInvertedOrTooLongRange() {
        assertThatThrownBy(() -> reports.revenue(LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-01"), "DAY"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("messageKey", ReportErrorKeys.INVALID_RANGE);
        assertThatThrownBy(() -> reports.revenue(LocalDate.parse("2025-01-01"), LocalDate.parse("2026-10-01"), "DAY"))
                .hasFieldOrPropertyWithValue("messageKey", ReportErrorKeys.INVALID_RANGE);
    }

    private UUID order(String status, String method, String placedAt, String subtotal, String discount, String shipping, String total) {
        UUID id = UUID.randomUUID();
        Timestamp at = Timestamp.from(OffsetDateTime.parse(placedAt).toInstant());
        jdbc.update("""
                insert into orders (id, code, customer_id, status, subtotal, discount, shipping_fee, total, recipient_name, contact_phone,
                    contact_email, shipping_address, payment_method, shipping_carrier, idempotency_key, placed_at, created_at, created_by,
                    updated_at, updated_by)
                values (?, ?, ?, ?, ?, ?, ?, ?, 'Khách', '0900000000', 'k@example.com', '{}'::jsonb, ?, 'GHN', ?, ?, ?, 'test', ?, 'test')
                """, id, "ORD-" + id.toString().substring(0, 8), UUID.randomUUID(), status, new BigDecimal(subtotal), new BigDecimal(discount),
                new BigDecimal(shipping), new BigDecimal(total), method, id.toString(), at, at, at);
        return id;
    }

    private void item(UUID orderId, UUID productId, String name, int quantity, String lineTotal) {
        Timestamp now = Timestamp.from(OffsetDateTime.now().toInstant());
        jdbc.update("""
                insert into order_item (id, order_id, sku_id, product_id, product_slug, product_name, unit_price, quantity, line_total,
                    created_at, created_by, updated_at, updated_by)
                values (?, ?, ?, ?, 'slug', ?, ?, ?, ?, ?, 'test', ?, 'test')
                """, UUID.randomUUID(), orderId, UUID.randomUUID(), productId, name, new BigDecimal(lineTotal).divide(BigDecimal.valueOf(quantity)),
                quantity, new BigDecimal(lineTotal), now, now);
    }
}
