package vn.ecommerce.order.shipping.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShippingErrorKeys {

    /** {0} = mã hãng. */
    public static final String CARRIER_NOT_FOUND = "shipping.carrier-not-found";
    public static final String SHIPMENT_EXISTS = "shipping.shipment-exists";
    public static final String INVALID_SIGNATURE = "shipping.invalid-signature";
    /** {0} = mã vận đơn. */
    public static final String TRACKING_NOT_FOUND = "shipping.tracking-not-found";
}
