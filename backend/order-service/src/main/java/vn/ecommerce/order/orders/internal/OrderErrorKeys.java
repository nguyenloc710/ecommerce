package vn.ecommerce.order.orders.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderErrorKeys {

    public static final String NOT_FOUND = "order.not-found";
    /** {0} = trạng thái hiện tại, {1} = trạng thái đích. */
    public static final String INVALID_TRANSITION = "order.invalid-transition";
    /** {0} = trạng thái hiện tại. */
    public static final String CANNOT_CANCEL = "order.cannot-cancel";
    public static final String ALREADY_PAID = "order.already-paid";
}
