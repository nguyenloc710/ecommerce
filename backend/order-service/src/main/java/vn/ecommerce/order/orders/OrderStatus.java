package vn.ecommerce.order.orders;

import java.util.EnumSet;
import java.util.Set;

/**
 * Vòng đời đơn hàng (B-04). Toàn bộ bảng chuyển trạng thái nằm ở đây; service gọi {@link #canTransitionTo} trước khi ghi lịch sử.
 */
public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    CONFIRMED,
    PACKING,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED;

    private Set<OrderStatus> next;

    static {
        PENDING.next = EnumSet.of(AWAITING_PAYMENT, CONFIRMED, CANCELLED);
        AWAITING_PAYMENT.next = EnumSet.of(CONFIRMED, CANCELLED);
        CONFIRMED.next = EnumSet.of(PACKING, CANCELLED);
        PACKING.next = EnumSet.of(SHIPPING, CANCELLED);
        SHIPPING.next = EnumSet.of(DELIVERED);
        DELIVERED.next = EnumSet.of(COMPLETED, RETURN_REQUESTED);
        COMPLETED.next = EnumSet.of(RETURN_REQUESTED);
        CANCELLED.next = EnumSet.noneOf(OrderStatus.class);
        RETURN_REQUESTED.next = EnumSet.of(RETURNED, COMPLETED);
        RETURNED.next = EnumSet.of(REFUNDED);
        REFUNDED.next = EnumSet.noneOf(OrderStatus.class);
    }

    public boolean canTransitionTo(OrderStatus target) {
        return next.contains(target);
    }

    public Set<OrderStatus> allowedNext() {
        return Set.copyOf(next);
    }

    /** Khách được tự hủy khi shop chưa đóng gói. */
    public boolean customerCancellable() {
        return this == PENDING || this == AWAITING_PAYMENT || this == CONFIRMED;
    }

    public boolean isFinal() {
        return next.isEmpty();
    }
}
