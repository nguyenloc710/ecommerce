package vn.ecommerce.customer.profile.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    /** Admin tìm theo email/tên/số điện thoại (chứa, không phân biệt hoa thường); {@code q} null = tất cả. */
    @Query(value = """
            select p from CustomerProfile p
            where (:q is null or lower(p.email) like :q or lower(p.fullName) like :q or p.phone like :q)
            order by p.createdAt desc
            """, countQuery = """
            select count(p) from CustomerProfile p
            where (:q is null or lower(p.email) like :q or lower(p.fullName) like :q or p.phone like :q)
            """)
    Page<CustomerProfile> search(@Param("q") String q, Pageable pageable);
}
