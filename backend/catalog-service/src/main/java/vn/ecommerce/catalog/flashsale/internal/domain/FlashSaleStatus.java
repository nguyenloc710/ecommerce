package vn.ecommerce.catalog.flashsale.internal.domain;

/** Trạng thái lưu của chương trình; sắp diễn ra / đang chạy / đã kết thúc suy ra từ giờ ({@link FlashSale#phase}). */
public enum FlashSaleStatus {
    ACTIVE,
    CANCELLED
}
