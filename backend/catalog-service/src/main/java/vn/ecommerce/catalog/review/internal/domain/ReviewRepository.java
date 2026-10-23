package vn.ecommerce.catalog.review.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByCustomerIdAndProductId(String customerId, UUID productId);

    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStatusOrderByCreatedAtAsc(ReviewStatus status, Pageable pageable);

    /** Số đánh giá theo từng mức sao (chỉ đánh giá đã duyệt) để tính điểm trung bình và biểu đồ. */
    @Query("""
            select new vn.ecommerce.catalog.review.internal.domain.RatingCount(r.rating, count(r))
            from Review r where r.productId = :productId and r.status = vn.ecommerce.catalog.review.internal.domain.ReviewStatus.APPROVED
            group by r.rating
            """)
    List<RatingCount> countApprovedByRating(@Param("productId") UUID productId);
}
