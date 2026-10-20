package vn.ecommerce.catalog.inventory.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryErrorKeys {

    /** {0} = danh sách SKU thiếu hàng. */
    public static final String INSUFFICIENT = "inventory.insufficient";
    /** {0} = danh sách SKU không tồn tại hoặc không bán. */
    public static final String SKU_NOT_SELLABLE = "inventory.sku-not-sellable";
    public static final String BUSY = "inventory.busy";
    public static final String ITEM_NOT_FOUND = "inventory.item-not-found";
    /** {0} = số đang giữ. */
    public static final String ON_HAND_BELOW_RESERVED = "inventory.on-hand-below-reserved";
    public static final String RESERVATION_NOT_FOUND = "inventory.reservation-not-found";
}
