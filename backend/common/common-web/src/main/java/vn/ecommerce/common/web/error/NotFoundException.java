package vn.ecommerce.common.web.error;

import org.springframework.http.HttpStatus;

/** Tài nguyên không tồn tại, hoặc tồn tại nhưng người dùng không được phép biết. */
public class NotFoundException extends ApiException {

    public NotFoundException(String messageKey, Object... args) {
        super(HttpStatus.NOT_FOUND, messageKey, args);
    }
}
