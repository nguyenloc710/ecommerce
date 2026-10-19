package vn.ecommerce.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình bảo mật riêng từng service.
 *
 * @param publicGetPaths đường dẫn GET không cần đăng nhập (ví dụ {@code /api/products/**})
 * @param publicPaths    đường dẫn mọi method không cần đăng nhập (ví dụ webhook xác thực bằng chữ ký)
 */
@ConfigurationProperties(prefix = "ecommerce.security")
public record SecurityProperties(List<String> publicGetPaths, List<String> publicPaths) {

    public SecurityProperties {
        publicGetPaths = publicGetPaths == null ? List.of() : List.copyOf(publicGetPaths);
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
