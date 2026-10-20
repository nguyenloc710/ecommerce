package vn.ecommerce.order.promotion.internal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Admin tạo/sửa voucher. {@code version} bắt buộc khi sửa. */
public record VoucherUpsertCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 30, message = "{validation.max-length}")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "{validation.enum}") String code,
        @NotNull(message = "{validation.required}") @Pattern(regexp = "PERCENT|FIXED", message = "{validation.enum}") String type,
        @NotNull(message = "{validation.required}") @DecimalMin(value = "0.01", message = "{validation.min}") BigDecimal value,
        @DecimalMin(value = "0", message = "{validation.min}") BigDecimal maxDiscount,
        @DecimalMin(value = "0", message = "{validation.min}") BigDecimal minSubtotal,
        @NotNull(message = "{validation.required}") Instant startsAt,
        @NotNull(message = "{validation.required}") Instant endsAt,
        @Min(value = 1, message = "{validation.min}") Integer usageLimit,
        @Min(value = 1, message = "{validation.min}") @Max(value = 100, message = "{validation.max-length}") Integer perCustomerLimit,
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "{validation.enum}") String status,
        @Size(max = 255, message = "{validation.max-length}") String description,
        Long version) {
}
