package vn.ecommerce.common.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Lấy người dùng hiện tại từ JWT trong {@code SecurityContext}. Id là {@code sub} của Keycloak,
 * dùng làm {@code customerId} ở mọi service. Không bao giờ nhận id người dùng từ request body.
 */
public class CurrentUser {

    /** {@code sub} của người dùng hiện tại, rỗng nếu request không có JWT (endpoint công khai). */
    public Optional<String> id() {
        return jwt().map(Jwt::getSubject);
    }

    /** {@code sub} của người dùng hiện tại; dùng ở endpoint bắt buộc đăng nhập. */
    public String requireId() {
        return id().orElseThrow(() -> new IllegalStateException("Endpoint yêu cầu đăng nhập nhưng không có JWT"));
    }

    public Optional<String> email() {
        return claim("email");
    }

    /** Tên hiển thị: claim {@code name}, không có thì {@code preferred_username}. */
    public Optional<String> displayName() {
        return claim("name").or(() -> claim("preferred_username"));
    }

    public Optional<String> claim(String name) {
        return jwt().map(token -> token.getClaimAsString(name)).filter(v -> v != null && !v.isBlank());
    }

    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

    private Optional<Jwt> jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return Optional.of(token.getToken());
        }
        return Optional.empty();
    }
}
