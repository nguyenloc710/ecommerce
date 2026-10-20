package vn.ecommerce.order.checkout.internal;

import vn.ecommerce.order.checkout.CheckoutQuoteView;
import vn.ecommerce.order.checkout.CheckoutResult;

/** Luồng checkout 7.5 của thiết kế cho khách đang đăng nhập. */
public interface CheckoutService {

    /** Báo giá: không giữ hàng, không tạo gì; mã giảm giá lỗi trả về trong {@code voucherError}. */
    CheckoutQuoteView quote(CheckoutRequest request);

    /**
     * Đặt hàng: tra giá → voucher → phí ship → giữ hàng ở catalog → ghi đơn + payment (một transaction) → dọn giỏ.
     * Cùng {@code idempotencyKey} gọi lại trả đơn cũ.
     */
    CheckoutResult placeOrder(CheckoutRequest request, String idempotencyKey);
}
