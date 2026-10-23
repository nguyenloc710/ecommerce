package vn.ecommerce.catalog.inventory.internal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Giữ hàng cho một đơn; idempotent theo {@code orderId} (gọi lại trả kết quả cũ).
 *
 * @param customerId {@code sub} của khách đặt đơn, để giới hạn suất flash sale theo khách
 */
public record ReserveRequest(
        @NotNull(message = "{validation.required}") UUID orderId,
        @Size(max = 64, message = "{validation.max-length}") String customerId,
        @NotEmpty(message = "{validation.required}") @Size(max = 100, message = "{validation.max-length}") List<@Valid Line> lines) {

    /** @param unitPrice đơn giá đã báo cho khách; thấp bằng giá flash sale thì cần giữ suất flash sale */
    public record Line(
            @NotNull(message = "{validation.required}") UUID skuId,
            @Min(value = 1, message = "{validation.min}") @Max(value = 999, message = "{validation.max-length}") int quantity,
            @PositiveOrZero(message = "{validation.min}") BigDecimal unitPrice) {

        public Line(UUID skuId, int quantity) {
            this(skuId, quantity, null);
        }
    }
}
