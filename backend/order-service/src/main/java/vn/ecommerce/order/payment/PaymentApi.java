package vn.ecommerce.order.payment;

import java.util.Set;

/** API public của module payment cho checkout. */
public interface PaymentApi {

    /** Tạo bản ghi thanh toán cho đơn; với cổng online trả thêm URL để khách sang trang thanh toán. */
    PaymentInitResult initiate(PaymentRequest request);

    /** Phương thức đang bật (có bean cổng tương ứng); cổng thật chưa cấu hình credential thì không có ở đây. */
    Set<PaymentMethod> availableMethods();
}
