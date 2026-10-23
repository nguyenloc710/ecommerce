package vn.ecommerce.order.orders.internal.report;

import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import vn.ecommerce.order.orders.OrderStatus;

/** Nhóm trạng thái đơn dùng trong báo cáo doanh thu. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RevenueStatuses {

    /** Đã xác nhận (COD) hoặc đã thanh toán, chưa trả hàng: tính vào doanh thu. */
    static final Set<OrderStatus> COUNTED = Set.of(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPING, OrderStatus.DELIVERED,
            OrderStatus.COMPLETED, OrderStatus.RETURN_REQUESTED);
    static final Set<OrderStatus> AWAITING = Set.of(OrderStatus.PENDING, OrderStatus.AWAITING_PAYMENT);
    static final Set<OrderStatus> RETURNED = Set.of(OrderStatus.RETURNED, OrderStatus.REFUNDED);

    static final List<String> COUNTED_NAMES = COUNTED.stream().map(Enum::name).sorted().toList();
}
