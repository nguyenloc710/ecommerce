package vn.ecommerce.order.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import vn.ecommerce.order.shipping.ShippingOption;

/**
 * Bảng tính trước khi đặt: giá hiện tại từng dòng, giảm giá, các lựa chọn giao hàng, tổng.
 *
 * @param voucherError message key khi mã không áp dụng được (FE hiển thị, không chặn báo giá)
 * @param paymentMethods phương thức thanh toán đang bật (COD, MOCK_ONLINE, VNPAY...) để FE chỉ hiện lựa chọn dùng được
 */
public record CheckoutQuoteView(List<Line> lines, BigDecimal subtotal, String voucherCode, BigDecimal discount, String voucherError,
                                List<ShippingOption> shippingOptions, ShippingOption selectedShipping, BigDecimal shippingFee,
                                BigDecimal total, List<String> paymentMethods) {

    public record Line(UUID skuId, String productSlug, String productName, String variantLabel, String imageUrl,
                       BigDecimal unitPrice, int quantity, String engraving, BigDecimal lineTotal) {
    }
}
