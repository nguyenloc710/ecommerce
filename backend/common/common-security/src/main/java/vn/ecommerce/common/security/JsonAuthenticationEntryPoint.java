package vn.ecommerce.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import vn.ecommerce.common.web.error.CommonErrorKeys;

/** 401: giữ header {@code WWW-Authenticate} chuẩn Bearer của Spring, thêm body {@code ErrorResponse}. */
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
    private final SecurityErrorWriter writer;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        bearer.commence(request, response, exception);
        writer.write(request, response, HttpStatus.UNAUTHORIZED, CommonErrorKeys.UNAUTHORIZED);
    }
}
