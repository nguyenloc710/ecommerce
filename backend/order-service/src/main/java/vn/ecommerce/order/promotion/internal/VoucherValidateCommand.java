package vn.ecommerce.order.promotion.internal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Khách kiểm tra mã trước khi đặt; {@code subtotal} là tạm tính FE đang hiển thị (checkout sẽ tính lại). */
public record VoucherValidateCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 30, message = "{validation.max-length}") String code,
        @NotNull(message = "{validation.required}") @DecimalMin(value = "0", message = "{validation.min}") BigDecimal subtotal) {
}
