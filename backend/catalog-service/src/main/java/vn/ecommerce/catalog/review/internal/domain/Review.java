package vn.ecommerce.catalog.review.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.ecommerce.common.jpa.AuditableEntity;

/**
 * Đánh giá của khách đã nhận hàng (B-10). Mỗi khách một đánh giá cho mỗi sản phẩm; chỉ APPROVED mới hiện công khai.
 * Ẩn đánh giá đã duyệt = chuyển REJECTED kèm lý do (không xóa, giữ lịch sử kiểm duyệt).
 */
@Getter
@Entity
@Table(name = "review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends AuditableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false)
    private short rating;

    @Column(length = 2000)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_keys", nullable = false)
    private List<String> imageKeys = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "moderated_by", length = 64)
    private String moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    public Review(ReviewEligibility eligibility, String authorName, int rating, String content, List<String> imageKeys) {
        this.productId = eligibility.getProductId();
        this.skuId = eligibility.getSkuId();
        this.customerId = eligibility.getCustomerId();
        this.orderId = eligibility.getOrderId();
        this.authorName = authorName;
        this.rating = (short) rating;
        this.content = content;
        this.imageKeys = new ArrayList<>(imageKeys);
    }

    /** Kiểm duyệt: trả false nếu đã ở trạng thái đích (gọi lại không ghi gì). */
    public boolean moderate(ReviewStatus target, String reason, String moderator) {
        if (status == target && (target != ReviewStatus.REJECTED || Objects.equals(rejectReason, reason))) {
            return false;
        }
        this.status = target;
        this.rejectReason = target == ReviewStatus.REJECTED ? reason : null;
        this.moderatedBy = moderator;
        this.moderatedAt = Instant.now();
        return true;
    }
}
