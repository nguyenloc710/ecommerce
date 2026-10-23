package vn.ecommerce.catalog.flashsale.internal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** SKU đang trong flash sale chạy lúc này, kèm số suất đang bị đơn chưa chốt giữ. */
public record ActiveItem(UUID itemId, UUID skuId, BigDecimal salePrice, int quota, int sold, int perCustomerLimit, Instant endsAt,
                         long held) {

    /** Suất còn bán được theo DB (nguồn sự thật, dùng khởi tạo bộ đếm Valkey). */
    public long remaining() {
        return Math.max(0, quota - sold - held);
    }
}
