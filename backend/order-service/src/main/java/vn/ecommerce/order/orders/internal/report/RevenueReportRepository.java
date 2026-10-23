package vn.ecommerce.order.orders.internal.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.order.orders.internal.Order;

/**
 * Truy vấn tổng hợp cho báo cáo doanh thu (chỉ đọc). Native SQL vì cần {@code date_trunc ... AT TIME ZONE} để gom theo
 * ngày/tháng giờ Việt Nam; khoảng thời gian là {@code [from, to)} theo {@code placed_at}, dùng index (status, placed_at).
 */
public interface RevenueReportRepository extends Repository<Order, UUID> {

    /** Số đơn và các khoản tiền theo trạng thái, để tính tổng doanh thu, đơn hủy, đơn trả. */
    @Query(value = """
            select status as status, count(*) as orderCount, coalesce(sum(subtotal), 0) as subtotal, coalesce(sum(discount), 0) as discount,
                   coalesce(sum(shipping_fee), 0) as shippingFee, coalesce(sum(total), 0) as total
            from orders
            where placed_at >= :from and placed_at < :to
            group by status
            """, nativeQuery = true)
    List<StatusTotals> totalsByStatus(@Param("from") Instant from, @Param("to") Instant to);

    /** Doanh thu theo kỳ ({@code unit} = day | month), kỳ là ngày đầu kỳ dạng yyyy-MM-dd theo múi giờ {@code zone}. */
    @Query(value = """
            select to_char(date_trunc(:unit, placed_at at time zone :zone), 'YYYY-MM-DD') as period, count(*) as orderCount,
                   coalesce(sum(total), 0) as netRevenue
            from orders
            where status in (:statuses) and placed_at >= :from and placed_at < :to
            group by 1
            order by 1
            """, nativeQuery = true)
    List<PeriodTotals> totalsByPeriod(@Param("unit") String unit, @Param("zone") String zone, @Param("statuses") Collection<String> statuses,
                                      @Param("from") Instant from, @Param("to") Instant to);

    /** Sản phẩm bán chạy theo doanh thu dòng hàng (trước giảm giá đơn). */
    @Query(value = """
            select i.product_id as productId, max(i.product_name) as productName, max(i.product_slug) as productSlug,
                   sum(i.quantity) as quantity, sum(i.line_total) as revenue
            from order_item i join orders o on o.id = i.order_id
            where o.status in (:statuses) and o.placed_at >= :from and o.placed_at < :to
            group by i.product_id
            order by revenue desc, quantity desc
            limit :limit
            """, nativeQuery = true)
    List<ProductTotals> topProducts(@Param("statuses") Collection<String> statuses, @Param("from") Instant from, @Param("to") Instant to,
                                    @Param("limit") int limit);

    @Query(value = """
            select payment_method as method, count(*) as orderCount, coalesce(sum(total), 0) as netRevenue
            from orders
            where status in (:statuses) and placed_at >= :from and placed_at < :to
            group by payment_method
            order by netRevenue desc
            """, nativeQuery = true)
    List<MethodTotals> totalsByPaymentMethod(@Param("statuses") Collection<String> statuses, @Param("from") Instant from,
                                             @Param("to") Instant to);

    interface StatusTotals {
        String getStatus();

        long getOrderCount();

        BigDecimal getSubtotal();

        BigDecimal getDiscount();

        BigDecimal getShippingFee();

        BigDecimal getTotal();
    }

    interface PeriodTotals {
        String getPeriod();

        long getOrderCount();

        BigDecimal getNetRevenue();
    }

    interface ProductTotals {
        UUID getProductId();

        String getProductName();

        String getProductSlug();

        long getQuantity();

        BigDecimal getRevenue();
    }

    interface MethodTotals {
        String getMethod();

        long getOrderCount();

        BigDecimal getNetRevenue();
    }
}
