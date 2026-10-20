package vn.ecommerce.order.promotion.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, UUID> {

    long countByVoucherIdAndCustomerId(UUID voucherId, UUID customerId);
}
