package vn.ecommerce.common.web.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Trả header {@code X-Trace-Id} trên mọi response để khách hoặc FE gửi kèm khi báo lỗi.
 * Chạy sau filter tracing của Micrometer nên MDC đã có {@code traceId}.
 */
public class TraceIdResponseFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId != null) {
            response.setHeader(TRACE_ID_HEADER, traceId);
        }
        chain.doFilter(request, response);
    }
}
