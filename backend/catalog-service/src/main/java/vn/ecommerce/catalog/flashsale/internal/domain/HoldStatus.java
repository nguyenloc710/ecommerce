package vn.ecommerce.catalog.flashsale.internal.domain;

/** HELD → COMMITTED (đơn xác nhận) hoặc RELEASED (đơn không thành); COMMITTED → RELEASED khi hủy đơn đã chốt. */
public enum HoldStatus {
    HELD,
    COMMITTED,
    RELEASED
}
