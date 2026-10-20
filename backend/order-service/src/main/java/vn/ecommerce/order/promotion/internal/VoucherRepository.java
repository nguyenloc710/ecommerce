package vn.ecommerce.order.promotion.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query(value = "select v from Voucher v where (:q is null or upper(v.code) like :q) order by v.createdAt desc",
            countQuery = "select count(v) from Voucher v where (:q is null or upper(v.code) like :q)")
    Page<Voucher> search(@Param("q") String q, Pageable pageable);
}
