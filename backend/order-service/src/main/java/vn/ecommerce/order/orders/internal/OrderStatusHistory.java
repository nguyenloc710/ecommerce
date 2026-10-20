package vn.ecommerce.order.orders.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.order.orders.OrderStatus;

/** Một lần đổi trạng thái: ai, lúc nào, lý do. Bất biến. */
@Getter
@Entity
@Table(name = "order_status_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private OrderStatus toStatus;

    private String reason;

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    OrderStatusHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus, String reason, String actorId) {
        this.id = UUID.randomUUID();
        this.order = order;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.actorId = actorId;
        this.createdAt = Instant.now();
    }
}
