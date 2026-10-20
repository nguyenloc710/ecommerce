package vn.ecommerce.catalog.inventory.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Tồn kho một SKU (một kho duy nhất ở MVP). Chỉ tham chiếu SKU bằng id, không join sang module product. */
@Getter
@Entity
@Table(name = "inventory_item")
@Audited
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem extends AuditableEntity {

    @Column(name = "sku_id", nullable = false, updatable = false)
    private UUID skuId;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(nullable = false)
    private int reserved;

    @Setter
    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold;

    public InventoryItem(UUID skuId, int lowStockThreshold) {
        this.skuId = skuId;
        this.lowStockThreshold = lowStockThreshold;
    }

    public int available() {
        return onHand - reserved;
    }

    public boolean canReserve(int quantity) {
        return available() >= quantity;
    }

    /** Giữ hàng: tăng reserved, on_hand giữ nguyên. Gọi sau khi đã {@link #canReserve}. */
    public void reserve(int quantity) {
        this.reserved += quantity;
    }

    /** Nhả hàng đã giữ (hủy đơn, hết hạn). */
    public void release(int quantity) {
        this.reserved = Math.max(0, this.reserved - quantity);
    }

    /** Chốt bán: trừ cả on_hand và reserved. */
    public void commit(int quantity) {
        this.reserved = Math.max(0, this.reserved - quantity);
        this.onHand = Math.max(0, this.onHand - quantity);
    }

    /** Bán thẳng từ on_hand khi reservation đã bị nhả (thanh toán về trễ) nhưng hàng vẫn còn. */
    public void deduct(int quantity) {
        this.onHand -= quantity;
    }

    /** Admin đặt lại số tồn tuyệt đối (kiểm kê, nhập hàng). Service kiểm tra không thấp hơn số đang giữ. */
    public void setOnHand(int onHand) {
        this.onHand = onHand;
    }
}
