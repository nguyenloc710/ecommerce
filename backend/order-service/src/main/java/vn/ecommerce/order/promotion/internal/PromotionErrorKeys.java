package vn.ecommerce.order.promotion.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromotionErrorKeys {

    public static final String NOT_FOUND = "promotion.voucher-not-found";
    public static final String INACTIVE = "promotion.voucher-inactive";
    public static final String NOT_STARTED = "promotion.voucher-not-started";
    public static final String EXPIRED = "promotion.voucher-expired";
    public static final String EXHAUSTED = "promotion.voucher-exhausted";
    public static final String ALREADY_USED = "promotion.voucher-used";
    /** {0} = tạm tính tối thiểu. */
    public static final String MIN_SUBTOTAL = "promotion.min-subtotal";
    /** {0} = mã. */
    public static final String CODE_TAKEN = "promotion.code-taken";
    public static final String INVALID_PERIOD = "promotion.invalid-period";
    public static final String PERCENT_RANGE = "promotion.percent-range";
}
