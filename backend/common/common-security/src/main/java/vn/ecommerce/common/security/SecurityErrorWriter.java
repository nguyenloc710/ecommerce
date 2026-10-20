package vn.ecommerce.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;
import vn.ecommerce.common.web.error.ErrorResponse;

/**
 * Ghi {@link ErrorResponse} cho lỗi 401/403 phát sinh trong security filter (trước khi tới controller),
 * để FE nhận một định dạng lỗi duy nhất. Locale lấy từ {@code Accept-Language} của request.
 */
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    private final JsonMapper jsonMapper;
    private final MessageSource messageSource;

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code) throws IOException {
        String message = messageSource.getMessage(code, null, code, request.getLocale());
        ErrorResponse body = new ErrorResponse(code, message, MDC.get(TRACE_ID_MDC_KEY), Instant.now(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
