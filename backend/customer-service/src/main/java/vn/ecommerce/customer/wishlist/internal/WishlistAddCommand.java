package vn.ecommerce.customer.wishlist.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Slug sản phẩm để storefront dựng link; không kiểm tra với catalog (mục không còn bán sẽ bị ẩn khi hiển thị). */
public record WishlistAddCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 160, message = "{validation.max-length}") String productSlug) {
}
