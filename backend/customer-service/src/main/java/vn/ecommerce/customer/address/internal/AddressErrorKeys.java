package vn.ecommerce.customer.address.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AddressErrorKeys {

    public static final String NOT_FOUND = "address.not-found";
    /** {0} = mã tỉnh/thành. */
    public static final String PROVINCE_NOT_FOUND = "address.province-not-found";
    /** {0} = mã phường/xã, {1} = mã tỉnh/thành. */
    public static final String WARD_NOT_IN_PROVINCE = "address.ward-not-in-province";
    /** {0} = số địa chỉ tối đa. */
    public static final String LIMIT_REACHED = "address.limit-reached";
}
