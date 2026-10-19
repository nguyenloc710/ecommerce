package vn.ecommerce.common.web.error;

import org.springframework.http.HttpStatus;

/** Đã đăng nhập nhưng không có quyền trên tài nguyên này. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String messageKey, Object... args) {
        super(HttpStatus.FORBIDDEN, messageKey, args);
    }
}
