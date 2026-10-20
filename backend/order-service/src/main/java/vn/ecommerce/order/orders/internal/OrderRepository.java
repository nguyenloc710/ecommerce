package vn.ecommerce.order.orders.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.order.orders.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByCode(String code);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByCodeAndCustomerId(String code, UUID customerId);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    @EntityGraph(attributePaths = "items")
    Page<Order> findByCustomerIdOrderByPlacedAtDesc(UUID customerId, Pageable pageable);

    /** Admin: lọc theo tab trạng thái và tìm theo mã đơn, email, SĐT, tên người nhận. */
    @EntityGraph(attributePaths = "items")
    @Query(value = """
            select o from Order o
            where (:status is null or o.status = :status)
              and (:q is null or lower(o.code) like :q or lower(o.contactEmail) like :q or o.contactPhone like :q or lower(o.recipientName) like :q)
            order by o.placedAt desc
            """, countQuery = """
            select count(o) from Order o
            where (:status is null or o.status = :status)
              and (:q is null or lower(o.code) like :q or lower(o.contactEmail) like :q or o.contactPhone like :q or lower(o.recipientName) like :q)
            """)
    Page<Order> search(@Param("status") OrderStatus status, @Param("q") String q, Pageable pageable);

    /** Đơn online chưa thanh toán quá hạn (B-20). */
    List<Order> findTop200ByStatusAndPlacedAtBefore(OrderStatus status, Instant placedBefore);
}
