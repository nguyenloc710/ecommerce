package vn.ecommerce.order.cart.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CartErrorKeys {

    public static final String SKU_NOT_FOUND = "cart.sku-not-found";
    /** {0} = số dòng tối đa. */
    public static final String TOO_MANY_LINES = "cart.too-many-lines";
    /** {0} = số lượng tối đa mỗi dòng. */
    public static final String QUANTITY_EXCEEDED = "cart.quantity-exceeded";
}
