package vn.ecommerce.order.cart.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ecommerce.common.jpa.AuditableEntity;

@Getter
@Entity
@Table(name = "cart_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, updatable = false)
    private Cart cart;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private UUID skuId;

    @Column(name = "product_slug", nullable = false, length = 160)
    private String productSlug;

    @Setter
    @Column(nullable = false)
    private int quantity;

    /** B-08: khắc tên, NULL = không khắc; là một phần của khóa dòng nên không đổi sau khi tạo. */
    @Column(length = 10, updatable = false)
    private String engraving;

    CartItem(Cart cart, UUID skuId, String productSlug, int quantity, String engraving) {
        this.cart = cart;
        this.skuId = skuId;
        this.productSlug = productSlug;
        this.quantity = quantity;
        this.engraving = engraving;
    }
}
