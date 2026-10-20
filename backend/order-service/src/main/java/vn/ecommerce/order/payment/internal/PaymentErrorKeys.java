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
}
