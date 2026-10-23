package vn.ecommerce.catalog.flashsale.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FlashSaleConstants {

    /** Nhãn giá FE dịch thành "Flash sale" (xem {@code PriceAdjuster.AdjustedPrice#label}). */
    static final String PRICE_LABEL = "FLASH_SALE";
    static final String AUDIT_TARGET = "FLASH_SALE";
    static final String AUDIT_CREATE = "FLASH_SALE_CREATE";
    static final String AUDIT_CANCEL = "FLASH_SALE_CANCEL";
    /** Khóa Valkey; {@code {id}} là hash tag để hai khóa của một item cùng slot nếu sau này chạy cluster. */
    static final String REMAINING_KEY = "flash:{%s}:remaining";
    static final String CUSTOMER_KEY = "flash:{%s}:cust:%s";
    /** Khách không xác định (bên gọi cũ không gửi customerId): dùng chung một khóa, không áp giới hạn theo khách. */
    static final String ANONYMOUS_CUSTOMER = "_";
}
