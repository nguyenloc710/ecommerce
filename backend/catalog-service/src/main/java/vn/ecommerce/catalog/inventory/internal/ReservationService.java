package vn.ecommerce.catalog.inventory.internal;

import java.util.List;
import java.util.UUID;
import vn.ecommerce.catalog.inventory.internal.dto.ReservationResult;
import vn.ecommerce.catalog.inventory.internal.dto.ReserveRequest;

/**
 * Giữ / chốt / nhả hàng theo đơn. Mọi thao tác idempotent theo {@code orderId}; xung đột optimistic lock được
 * thử lại tự động (xem {@link InventoryProperties#optimisticRetries()}).
 */
public interface ReservationService {

    /**
     * Giữ hàng cho toàn bộ dòng, tất cả hoặc không gì cả.
     *
     * @throws vn.ecommerce.common.web.error.ConflictException {@code inventory.insufficient} khi thiếu hàng,
     *         {@code inventory.sku-not-sellable} khi SKU không bán, {@code inventory.busy} khi thử lại quá số lần
     */
    ReservationResult reserve(UUID orderId, String customerId, List<ReserveRequest.Line> lines);

    /** Giữ hàng không gắn khách (giới hạn mỗi khách của flash sale không áp dụng). */
    default ReservationResult reserve(UUID orderId, List<ReserveRequest.Line> lines) {
        return reserve(orderId, null, lines);
    }

    ReservationResult commit(UUID orderId);

    ReservationResult release(UUID orderId);

    ReservationResult status(UUID orderId);

    /** Dùng cho consumer event: ghi sổ đã xử lý + chốt hàng trong cùng transaction. Trả false nếu event đã xử lý. */
    boolean commitFromEvent(UUID eventId, UUID orderId);

    boolean releaseFromEvent(UUID eventId, UUID orderId);

    /** Nhả các đơn đã quá hạn giữ hàng; trả về số đơn đã nhả. */
    int releaseExpired();
}
