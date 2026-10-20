package vn.ecommerce.order.payment.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ecommerce.order.payment.PaymentMethod;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findByProviderAndProviderTxnId(PaymentMethod provider, String providerTxnId);
}
