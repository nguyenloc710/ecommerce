package vn.ecommerce.customer.staffadmin.internal.client;

import java.util.List;
import java.util.Optional;

/**
 * Adapter ra Keycloak Admin API; nghiệp vụ chỉ phụ thuộc interface này để test mock được.
 * Lỗi mạng/5xx được đổi thành {@link KeycloakUnavailableException}.
 */
public interface KeycloakAdminGateway {

    Optional<KeycloakUser> findUser(String userId);

    /** {@code enabled=false} chặn đăng nhập; token đang có vẫn sống đến khi hết hạn nên kèm {@link #logoutAllSessions}. */
    void setEnabled(String userId, boolean enabled);

    void logoutAllSessions(String userId);

    /**
     * Tạo nhân viên trong group nhân viên với mật khẩu tạm, buộc đổi ở lần đăng nhập đầu.
     *
     * @return id user vừa tạo
     * @throws vn.ecommerce.common.web.error.ConflictException email đã tồn tại
     */
    String createStaff(String email, String fullName, String temporaryPassword);

    /** Thành viên group nhân viên, phân trang theo {@code first}/{@code max} của Keycloak. */
    List<KeycloakUser> listStaff(int first, int max);
}
