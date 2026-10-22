package vn.ecommerce.order.payment.internal;

import org.springframework.http.HttpStatus;
import vn.ecommerce.common.web.error.ApiException;

/** Cổng thanh toán không trả lời hoặc từ chối tạo giao dịch: khách thấy 502, đơn không được tạo, hàng được nhả ngay. */
class PaymentGatewayUnavailableException extends ApiException {

    PaymentGatewayUnavailableException(String provider) {
        super(HttpStatus.BAD_GATEWAY, PaymentErrorKeys.GATEWAY_UNAVAILABLE, provider);
    }
}
