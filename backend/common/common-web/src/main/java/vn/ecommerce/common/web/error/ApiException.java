package vn.ecommerce.common.web.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception nghiệp vụ có ngữ nghĩa HTTP. Service chỉ ném exception này (qua các lớp con),
 * {@link GlobalExceptionHandler} đổi thành {@link ErrorResponse}.
 * <p>
 * {@code messageKey} vừa là mã lỗi trả cho FE, vừa là key tra trong {@code messages*.properties};
 * {@code args} thay cho placeholder {@code {0}, {1}...} của message.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final transient Object[] args;

    protected ApiException(HttpStatus status, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }
}
