package vn.ecommerce.customer.wishlist.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Mục yêu thích; dữ liệu tạm, bỏ thích là xóa cứng. */
@Getter
@Entity
@Table(name = "wishlist_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishlistItem extends AuditableEntity {

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private UUID skuId;

    @Column(name = "product_slug", nullable = false, length = 160)
    private String productSlug;

    public WishlistItem(UUID customerId, UUID skuId, String productSlug) {
        this.customerId = customerId;
        this.skuId = skuId;
        this.productSlug = productSlug;
    }
}
