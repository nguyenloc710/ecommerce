package vn.ecommerce.catalog.review.internal.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewEligibilityRepository extends JpaRepository<ReviewEligibility, UUID> {

    boolean existsByOrderIdAndSkuId(UUID orderId, UUID skuId);

    /** Lần nhận hàng gần nhất của khách với sản phẩm này (đánh giá gắn với đơn đó). */
    Optional<ReviewEligibility> findFirstByCustomerIdAndProductIdOrderByDeliveredAtDesc(String customerId, UUID productId);
}
