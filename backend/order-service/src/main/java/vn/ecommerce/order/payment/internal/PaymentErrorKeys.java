package vn.ecommerce.order.payment.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentErrorKeys {

    /** {0} = phương thức. */
    public static final String METHOD_NOT_SUPPORTED = "payment.method-not-supported";
    public static final String INVALID_SIGNATURE = "payment.invalid-signature";
    public static final String NOT_FOUND = "payment.not-found";
    public static final String AMOUNT_MISMATCH = "payment.amount-mismatch";
    /** {0} = cổng (MOMO...). */
    public static final String GATEWAY_UNAVAILABLE = "payment.gateway-unavailable";
    public static final String REFUND_NOT_FOUND = "payment.refund-not-found";
    /** {0} = mã đơn. */
    public static final String REFUND_ALREADY_DONE = "payment.refund-already-done";
}
