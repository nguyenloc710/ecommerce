package vn.ecommerce.catalog.flashsale.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlashSaleErrorKeys {

    public static final String NOT_FOUND = "flashsale.not-found";
    /** Giờ kết thúc phải sau giờ bắt đầu và sau hiện tại. */
    public static final String INVALID_WINDOW = "flashsale.invalid-window";
    /** {0} = số SKU tối đa mỗi chương trình. */
    public static final String TOO_MANY_ITEMS = "flashsale.too-many-items";
    /** {0} = id SKU bị lặp. */
    public static final String DUPLICATE_SKU = "flashsale.duplicate-sku";
    /** {0} = danh sách id SKU không bán được. */
    public static final String SKU_NOT_SELLABLE = "flashsale.sku-not-sellable";
    /** {0} = mã SKU có giá flash sale không thấp hơn giá niêm yết. */
    public static final String PRICE_NOT_LOWER = "flashsale.price-not-lower";
    /** {0} = danh sách id SKU đã nằm trong chương trình khác cùng khung giờ. */
    public static final String OVERLAP = "flashsale.overlap";
    public static final String ALREADY_ENDED = "flashsale.already-ended";
    /** {0} = id SKU hết suất. */
    public static final String SOLD_OUT = "flashsale.sold-out";
    /** {0} = số suất tối đa mỗi khách. */
    public static final String LIMIT_EXCEEDED = "flashsale.limit-exceeded";
    public static final String UNAVAILABLE = "flashsale.unavailable";
}
