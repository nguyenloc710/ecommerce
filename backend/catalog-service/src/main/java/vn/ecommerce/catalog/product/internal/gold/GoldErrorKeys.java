package vn.ecommerce.catalog.product.internal.gold;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GoldErrorKeys {

    /** Sản phẩm GOLD_RATE: variant phải có loại vàng, trọng lượng và phí gia công. */
    public static final String FIELDS_REQUIRED = "gold.fields-required";
    /** {0} = loại vàng chưa có giá. */
    public static final String RATE_MISSING = "gold.rate-missing";
}
