package vn.ecommerce.catalog.flashsale.internal;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Cổng trừ suất nhanh (Valkey + Lua). Chỉ là bộ đếm cache trước DB: khóa mất thì tự khởi tạo lại từ giá trị DB
 * mà bên gọi truyền vào. Lỗi kết nối → {@link FlashSaleUnavailableException}.
 */
interface FlashSaleQuota {

    /** Trừ suất tất cả hoặc không gì cả (nguyên tử trong một script). */
    AcquireResult acquire(String customerId, List<Claim> claims);

    /** Trả suất; khóa đã hết hạn thì bỏ qua (lần khởi tạo sau đọc từ DB). */
    void release(String customerId, List<Taken> taken);

    /**
     * @param perCustomerLimit 0 = không giới hạn theo khách
     * @param initialRemaining suất còn theo DB, dùng khi khóa chưa có
     * @param initialUsed      khách đã giữ/mua theo DB, dùng khi khóa chưa có
     * @param ttl              thời gian sống của khóa khi tạo mới
     */
    record Claim(UUID itemId, int quantity, int perCustomerLimit, long initialRemaining, long initialUsed, Duration ttl) {
    }

    record Taken(UUID itemId, int quantity) {
    }

    /**
     * @param rejectedItemId item bị từ chối; null khi thành công
     * @param limitExceeded  true = vượt giới hạn theo khách, false = hết suất
     */
    record AcquireResult(UUID rejectedItemId, boolean limitExceeded) {

        static final AcquireResult OK = new AcquireResult(null, false);

        boolean ok() {
            return rejectedItemId == null;
        }
    }
}
