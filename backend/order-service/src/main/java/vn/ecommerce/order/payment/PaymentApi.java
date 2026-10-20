package vn.ecommerce.order.payment;

import java.math.BigDecimal;
import java.util.UUID;

/** API public của module payment cho checkout. */
public interface PaymentApi {

    /** Tạo bản ghi thanh toán cho đơn; với cổng online trả thêm URL để khách sang trang thanh toán. */
    PaymentInitResult initiate(UUID orderId, String orderCode, BigDecimal amount, PaymentMethod method);
}
