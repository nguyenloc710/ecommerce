package vn.ecommerce.common.web.error;

import org.springframework.http.HttpStatus;

/** Dữ liệu hợp lệ về hình dạng nhưng sai nghiệp vụ (ví dụ voucher hết hạn). */
public class BadRequestException extends ApiException {

    public BadRequestException(String messageKey, Object... args) {
        super(HttpStatus.BAD_REQUEST, messageKey, args);
    }
}
