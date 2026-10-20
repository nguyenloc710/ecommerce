package vn.ecommerce.order.shipping.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.ecommerce.common.jpa.AuditableEntity;

@Getter
@Entity
@Table(name = "shipment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment extends AuditableEntity {

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(nullable = false, length = 20)
    private String carrier;

    @Column(name = "tracking_code", nullable = false, length = 64)
    private String trackingCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    /** Payload webhook gần nhất của hãng, để đối soát. */
    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> raw;

    public Shipment(UUID orderId, String carrier, String trackingCode, BigDecimal fee) {
        this.orderId = orderId;
        this.carrier = carrier;
        this.trackingCode = trackingCode;
        this.fee = fee;
    }

    public enum ShipmentStatus {
        CREATED,
        IN_TRANSIT,
        DELIVERED,
        FAILED
    }
}
