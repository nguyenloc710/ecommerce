package vn.ecommerce.order.cart.internal;

import vn.ecommerce.order.cart.CartApi;
import vn.ecommerce.order.cart.CartView;

/** Giỏ của khách đang đăng nhập. */
public interface CartService extends CartApi {

    CartView getMine();

    /** Đặt số lượng tuyệt đối cho dòng (0 = xóa); dòng mới được kiểm tra SKU có tồn tại ở catalog. */
    CartView setLine(CartLineCommand command);

    CartView removeLine(java.util.UUID skuId, String engraving);

    CartView merge(CartMergeCommand command);

    void clearMine();
}
