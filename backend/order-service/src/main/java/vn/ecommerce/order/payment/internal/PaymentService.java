package vn.ecommerce.order.payment.internal;

import vn.ecommerce.order.payment.PaymentApi;
import vn.ecommerce.order.payment.PaymentMethod;

public interface PaymentService extends PaymentApi {

    /**
     * Xử lý webhook: verify chữ ký → idempotent theo mã giao dịch → parse → cập nhật payment → báo module orders.
     *
     * @throws vn.ecommerce.common.web.error.ForbiddenException {@code payment.invalid-signature}
     */
    void handleWebhook(PaymentMethod provider, String signature, String rawBody);
}
