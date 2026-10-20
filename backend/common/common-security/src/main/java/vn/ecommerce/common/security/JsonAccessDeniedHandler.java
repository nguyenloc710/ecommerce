package vn.ecommerce.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import vn.ecommerce.common.web.error.CommonErrorKeys;

/** 403 từ {@code authorizeHttpRequests} (đã đăng nhập nhưng thiếu role). */
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter writer;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        writer.write(request, response, HttpStatus.FORBIDDEN, CommonErrorKeys.ACCESS_DENIED);
    }
}
