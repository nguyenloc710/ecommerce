package vn.ecommerce.catalog.inventory.internal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Giữ hàng cho một đơn; idempotent theo {@code orderId} (gọi lại trả kết quả cũ). */
public record ReserveRequest(
        @NotNull(message = "{validation.required}") UUID orderId,
        @NotEmpty(message = "{validation.required}") @Size(max = 100, message = "{validation.max-length}") List<@Valid Line> lines) {

    public record Line(
            @NotNull(message = "{validation.required}") UUID skuId,
            @Min(value = 1, message = "{validation.min}") @Max(value = 999, message = "{validation.max-length}") int quantity) {
    }
}
