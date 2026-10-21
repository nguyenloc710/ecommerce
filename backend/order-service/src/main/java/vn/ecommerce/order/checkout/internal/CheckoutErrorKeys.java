package vn.ecommerce.order.checkout.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CheckoutErrorKeys {

    public static final String EMPTY_CART = "checkout.empty-cart";
    /** {0} = tên sản phẩm (hoặc skuId nếu không còn). */
    public static final String SKU_NOT_FOUND = "checkout.sku-not-found";
    public static final String SKU_NOT_SELLABLE = "checkout.sku-not-sellable";
    /** {0} = tên sản phẩm, {1} = số còn lại. */
    public static final String INSUFFICIENT_STOCK = "checkout.insufficient-stock";
    public static final String ADDRESS_REQUIRED = "checkout.address-required";
    public static final String ADDRESS_NOT_FOUND = "checkout.address-not-found";
    public static final String IDEMPOTENCY_KEY_REQUIRED = "checkout.idempotency-key-required";
    public static final String PAYMENT_METHOD_REQUIRED = "validation.required";
    /** {0} = phương thức; cổng chưa bật (thiếu credential) dù enum có. */
    public static final String PAYMENT_METHOD_UNAVAILABLE = "checkout.payment-method-unavailable";
}
