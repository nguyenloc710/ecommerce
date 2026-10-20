package vn.ecommerce.customer.wishlist;

import java.time.Instant;
import java.util.UUID;

/** Mục yêu thích; storefront ghép thêm tên/giá/ảnh từ catalog theo {@code skuId}. */
public record WishlistItemView(UUID id, UUID skuId, String productSlug, Instant createdAt) {
}
