package vn.ecommerce.catalog.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SPI chạy kèm vòng đời giữ hàng (module flashsale giữ suất giá khuyến mãi) mà module inventory không phụ thuộc ngược.
 * Mọi phương thức phải idempotent theo {@code orderId}: inventory có thể gọi lại khi retry hoặc khi event đến lặp.
 */
public interface ReservationGuard {

    /**
     * Gọi trước khi giữ tồn kho; ném {@link vn.ecommerce.common.web.error.ApiException} để từ chối cả đơn.
     *
     * @param customerId {@code sub} của khách (null nếu không có, ví dụ gọi nội bộ cũ)
     * @param lines      mỗi SKU một dòng (đã gộp số lượng)
     */
    void acquire(UUID orderId, String customerId, List<Line> lines);

    /** Đơn không thành (giữ hàng lỗi, hủy, hết hạn thanh toán, hủy sau khi đã chốt): trả lại phần đã giữ. */
    void release(UUID orderId);

    /** Đơn đã xác nhận: phần đã giữ thành đã bán. */
    void commit(UUID orderId);

    /**
     * @param unitPrice đơn giá order-service đã báo cho khách (tra từ catalog lúc checkout); null nếu bên gọi không gửi
     */
    record Line(UUID skuId, int quantity, BigDecimal unitPrice) {
    }
}
