package vn.ecommerce.common.web.error;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Đổi mọi exception thành {@link ErrorResponse}. Đây là nơi duy nhất log stack trace của lỗi hệ thống,
 * các tầng khác không log lặp lại.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /** Key MDC do Micrometer Tracing gắn vào mỗi request. */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    private final MessageSource messageSource;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Lỗi nghiệp vụ phía server: {}", ex.getMessageKey(), ex);
        } else {
            log.warn("Từ chối request: status={}, code={}", ex.getStatus().value(), ex.getMessageKey());
        }
        return build(ex.getStatus(), ex.getMessageKey(), ex.getArgs(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldViolation(
                        error.getField(),
                        messageSource.getMessage(error, LocaleContextHolder.getLocale())))
                .toList();
        return build(HttpStatus.BAD_REQUEST, CommonErrorKeys.VALIDATION_FAILED, new Object[0], violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Request body không đọc được: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return build(HttpStatus.BAD_REQUEST, CommonErrorKeys.MALFORMED_REQUEST, new Object[0], List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Từ chối truy cập: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, CommonErrorKeys.ACCESS_DENIED, new Object[0], List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Xung đột cập nhật đồng thời: {}", ex.getPersistentClassName());
        return build(HttpStatus.CONFLICT, CommonErrorKeys.CONCURRENT_MODIFICATION, new Object[0], List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Lỗi không mong đợi", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorKeys.INTERNAL_ERROR, new Object[0], List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String key, Object[] args,
                                                List<ErrorResponse.FieldViolation> violations) {
        String message = messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
        ErrorResponse body = new ErrorResponse(key, message, MDC.get(TRACE_ID_MDC_KEY), Instant.now(), violations);
        return ResponseEntity.status(status).body(body);
    }
}
