package vn.ecommerce.catalog.flashsale.internal.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID> {

    Page<FlashSale> findAllByOrderByStartsAtDesc(Pageable pageable);

    /** Chương trình đang chạy, sắp kết thúc trước. */
    List<FlashSale> findByStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByEndsAtAsc(FlashSaleStatus status, Instant startsAt,
                                                                                          Instant endsAt);
}
