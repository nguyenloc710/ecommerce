package vn.ecommerce.catalog.flashsale.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Suất flash sale một đơn đang giữ; nguồn sự thật để trả suất idempotent và khởi tạo lại bộ đếm Valkey. */
@Getter
@Entity
@Table(name = "flash_sale_hold")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashSaleHold extends AuditableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "flash_sale_item_id", nullable = false)
    private UUID flashSaleItemId;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HoldStatus status = HoldStatus.HELD;

    public FlashSaleHold(UUID orderId, UUID flashSaleItemId, String customerId, int quantity) {
        this.orderId = orderId;
        this.flashSaleItemId = flashSaleItemId;
        this.customerId = customerId;
        this.quantity = quantity;
    }

    /** HELD → COMMITTED; trả false nếu không ở HELD. */
    public boolean commit() {
        if (status != HoldStatus.HELD) {
            return false;
        }
        status = HoldStatus.COMMITTED;
        return true;
    }

    /** Trả trạng thái trước đó nếu vừa chuyển sang RELEASED, null nếu đã RELEASED từ trước. */
    public HoldStatus release() {
        if (status == HoldStatus.RELEASED) {
            return null;
        }
        HoldStatus before = status;
        status = HoldStatus.RELEASED;
        return before;
    }
}
