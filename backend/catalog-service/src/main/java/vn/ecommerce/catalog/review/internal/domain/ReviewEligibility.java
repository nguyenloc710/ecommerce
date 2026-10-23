package vn.ecommerce.catalog.review.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Quyền đánh giá: khách đã nhận SKU này trong đơn {@code orderId} (ghi khi nhận event order.delivered). */
@Getter
@Entity
@Table(name = "review_eligibility")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewEligibility extends AuditableEntity {

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;

    public ReviewEligibility(String customerId, UUID productId, UUID skuId, UUID orderId, Instant deliveredAt) {
        this.customerId = customerId;
        this.productId = productId;
        this.skuId = skuId;
        this.orderId = orderId;
        this.deliveredAt = deliveredAt;
    }
}
