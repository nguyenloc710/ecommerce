package vn.ecommerce.order.catalogclient;

import java.util.List;
import java.util.UUID;

/**
 * Cổng sang catalog-service. Lỗi mạng/5xx → {@link CatalogUnavailableException} (502); lỗi nghiệp vụ của catalog
 * (409 {@code inventory.insufficient}, {@code inventory.sku-not-sellable}...) được ném lại với đúng message key.
 */
public interface CatalogClient {

    /** Giá, tên, tồn kho hiện tại của các SKU; SKU không tồn tại thì không có trong kết quả. */
    List<SkuSnapshot> lookup(List<UUID> skuIds);

    /**
     * Giữ hàng cho đơn, tất cả hoặc không gì cả; idempotent theo {@code orderId}. Kèm {@code customerId} để catalog
     * giới hạn suất flash sale theo khách ({@code flashsale.sold-out}, {@code flashsale.limit-exceeded}).
     */
    ReservationOutcome reserve(UUID orderId, String customerId, List<ReserveLine> lines);

    /** Nhả hàng (idempotent); dùng khi tạo đơn thất bại sau khi đã giữ. Không ném lỗi nghiệp vụ. */
    void release(UUID orderId);
}
