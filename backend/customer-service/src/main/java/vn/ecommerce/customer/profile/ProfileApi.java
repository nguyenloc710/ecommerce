package vn.ecommerce.customer.profile;

import java.util.UUID;

/** API public của module profile cho module khác (address, wishlist, staffadmin). */
public interface ProfileApi {

    /** Hồ sơ của người dùng hiện tại; chưa có thì tạo từ claim JWT (JIT) và phát {@code customer.created}. */
    CustomerProfileView ensureCurrentProfile();

    /** Đổi trạng thái (khóa/mở) do admin; không tồn tại thì ném NotFound. */
    void setStatus(UUID customerId, CustomerStatus status);
}
