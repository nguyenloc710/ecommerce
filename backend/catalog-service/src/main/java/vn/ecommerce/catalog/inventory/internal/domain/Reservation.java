package vn.ecommerce.catalog.inventory.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Một dòng giữ hàng của một đơn cho một SKU. Hết {@code expiresAt} mà chưa commit thì job nhả. */
@Getter
@Entity
@Table(name = "reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends AuditableEntity {

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private UUID skuId;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public Reservation(UUID orderId, UUID skuId, int quantity, Instant expiresAt) {
        this.orderId = orderId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return status == ReservationStatus.RESERVED;
    }

    public void markCommitted() {
        this.status = ReservationStatus.COMMITTED;
    }

    public void markReleased() {
        this.status = ReservationStatus.RELEASED;
    }
}
