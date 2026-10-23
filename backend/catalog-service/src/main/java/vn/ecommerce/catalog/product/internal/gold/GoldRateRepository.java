package vn.ecommerce.catalog.product.internal.gold;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.ecommerce.catalog.product.internal.domain.GoldKarat;
import vn.ecommerce.catalog.product.internal.domain.GoldRate;

public interface GoldRateRepository extends JpaRepository<GoldRate, UUID> {

    Optional<GoldRate> findFirstByKaratOrderByEffectiveAtDesc(GoldKarat karat);

    /** Giá hiện hành của từng loại vàng (bản ghi mới nhất mỗi loại). */
    @Query("""
            select r from GoldRate r
            where r.effectiveAt = (select max(r2.effectiveAt) from GoldRate r2 where r2.karat = r.karat)
            order by r.karat
            """)
    List<GoldRate> findCurrent();

    Page<GoldRate> findAllByOrderByEffectiveAtDesc(Pageable pageable);

    Page<GoldRate> findByKaratOrderByEffectiveAtDesc(GoldKarat karat, Pageable pageable);
}
