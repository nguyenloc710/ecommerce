package vn.ecommerce.order.cart.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Giỏ khách vãng lai gửi lên khi đăng nhập; dòng trùng (sku + khắc) được cộng số lượng. */
public record CartMergeCommand(
        @NotNull(message = "{validation.required}") @Size(max = 100, message = "{validation.max-length}") List<@Valid CartLineCommand> lines) {
}
