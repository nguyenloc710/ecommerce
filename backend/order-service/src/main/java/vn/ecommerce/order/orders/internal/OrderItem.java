package vn.ecommerce.order.orders.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Snapshot dòng hàng: tên, biến thể, giá tại thời điểm đặt (không join sang catalog). */
@Getter
@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private UUID skuId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "product_slug", nullable = false, length = 160)
    private String productSlug;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_label", length = 200)
    private String variantLabel;

    @Column(name = "sku_code", length = 64)
    private String skuCode;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 10)
    private String engraving;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    OrderItem(Order order, UUID skuId, UUID productId, String productSlug, String productName, String variantLabel,
              String skuCode, String imageUrl, BigDecimal unitPrice, int quantity, String engraving) {
        this.order = order;
        this.skuId = skuId;
        this.productId = productId;
        this.productSlug = productSlug;
        this.productName = productName;
        this.variantLabel = variantLabel;
        this.skuCode = skuCode;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.engraving = engraving;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
