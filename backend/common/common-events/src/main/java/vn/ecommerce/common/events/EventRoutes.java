package vn.ecommerce.common.events;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Exchange và routing key dùng chung. Quy ước routing key: {@code <domain>.<event>.v<n>}.
 * Mỗi service bên nhận tự khai báo queue riêng và bind vào routing key cần nghe.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventRoutes {

    /** Topic exchange duy nhất cho mọi event nghiệp vụ. */
    public static final String EXCHANGE = "ecommerce.events";

    public static final String CUSTOMER_CREATED = "customer.created.v1";
    public static final String ORDER_CONFIRMED = "order.confirmed.v1";
    public static final String ORDER_CANCELLED = "order.cancelled.v1";
    public static final String ORDER_SHIPPED = "order.shipped.v1";
    public static final String ORDER_DELIVERED = "order.delivered.v1";
}
