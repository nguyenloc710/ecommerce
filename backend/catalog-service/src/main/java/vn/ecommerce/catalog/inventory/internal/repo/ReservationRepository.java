package vn.ecommerce.catalog.inventory.internal.repo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.catalog.inventory.internal.domain.Reservation;
import vn.ecommerce.catalog.inventory.internal.domain.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByOrderId(UUID orderId);

    /** Id các đơn có reservation RESERVED đã quá hạn (distinct), xử lý theo lô ở job nhả hàng. */
    @Query("""
            select distinct r.orderId from Reservation r
            where r.status = :status and r.expiresAt < :before
            """)
    List<UUID> findExpiredOrderIds(@Param("status") ReservationStatus status, @Param("before") Instant before, Pageable limit);
}
