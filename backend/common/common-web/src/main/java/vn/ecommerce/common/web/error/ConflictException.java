package vn.ecommerce.common.web.error;

import org.springframework.http.HttpStatus;

/** Xung đột trạng thái: trùng dữ liệu, hết hàng, dữ liệu đã bị người khác sửa (optimistic lock). */
public class ConflictException extends ApiException {

    public ConflictException(String messageKey, Object... args) {
        super(HttpStatus.CONFLICT, messageKey, args);
    }
}
