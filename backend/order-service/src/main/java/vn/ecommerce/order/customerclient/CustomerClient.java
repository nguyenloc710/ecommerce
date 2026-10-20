package vn.ecommerce.order.customerclient;

import java.util.Optional;
import java.util.UUID;

/** Cổng sang customer-service; lỗi mạng/5xx → {@link CustomerUnavailableException}. */
public interface CustomerClient {

    /** Địa chỉ trong sổ của khách; rỗng nếu không thuộc khách hoặc đã xóa. */
    Optional<CustomerAddress> findAddress(UUID customerId, UUID addressId);
}
