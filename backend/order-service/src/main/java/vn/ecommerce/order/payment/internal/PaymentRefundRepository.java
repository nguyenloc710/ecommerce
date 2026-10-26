package vn.ecommerce.order.payment.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {

    boolean existsByPaymentId(UUID paymentId);

    Page<PaymentRefund> findByStatusOrderByCreatedAtAsc(PaymentRefund.RefundStatus status, Pageable pageable);
}
