package vn.ecommerce.order.shipping.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByOrderId(UUID orderId);

    Optional<Shipment> findByCarrierAndTrackingCode(String carrier, String trackingCode);
}
