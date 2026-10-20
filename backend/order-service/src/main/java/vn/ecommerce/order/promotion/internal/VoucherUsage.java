package vn.ecommerce.order.promotion.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một lượt dùng voucher cho một đơn (unique theo đơn). */
@Getter
@Entity
@Table(name = "voucher_usage")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoucherUsage {

    @Id
    private UUID id;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public VoucherUsage(UUID voucherId, UUID orderId, UUID customerId, BigDecimal discount) {
        this.id = UUID.randomUUID();
        this.voucherId = voucherId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.discount = discount;
        this.createdAt = Instant.now();
    }
}
