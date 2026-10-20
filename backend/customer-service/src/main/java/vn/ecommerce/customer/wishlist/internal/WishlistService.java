package vn.ecommerce.customer.wishlist.internal;

import java.util.List;
import java.util.UUID;
import vn.ecommerce.customer.wishlist.WishlistItemView;

/** Wishlist của khách đang đăng nhập. */
public interface WishlistService {

    List<WishlistItemView> listMine();

    /** Idempotent: đã có thì trả mục cũ, không tạo thêm. */
    WishlistItemView add(UUID skuId, WishlistAddCommand command);

    /** Idempotent: không có cũng không lỗi. */
    void remove(UUID skuId);
}
