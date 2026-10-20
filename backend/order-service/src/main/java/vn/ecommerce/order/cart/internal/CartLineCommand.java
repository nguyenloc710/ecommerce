package vn.ecommerce.order.cart.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Đặt số lượng cho một dòng giỏ; {@code quantity = 0} xóa dòng. Khi gộp giỏ, số lượng được cộng dồn.
 *
 * @param engraving B-08: tối đa 10 ký tự, rỗng = không khắc
 */
public record CartLineCommand(
        @NotNull(message = "{validation.required}") UUID skuId,
        @NotBlank(message = "{validation.required}") @Size(max = 160, message = "{validation.max-length}") String productSlug,
        @Min(value = 0, message = "{validation.min}") @Max(value = 999, message = "{validation.max-length}") int quantity,
        @Size(max = 10, message = "{validation.max-length}") String engraving) {

    public String normalizedEngraving() {
        return engraving == null || engraving.isBlank() ? null : engraving.trim();
    }
}
