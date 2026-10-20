package vn.ecommerce.customer.wishlist.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<WishlistItem> findByCustomerIdAndSkuId(UUID customerId, UUID skuId);

    long deleteByCustomerIdAndSkuId(UUID customerId, UUID skuId);
}
