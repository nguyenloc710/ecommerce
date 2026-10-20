package vn.ecommerce.catalog.product.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Ảnh của sản phẩm; gắn {@code variant} khi ảnh theo màu. Xóa cứng vì đơn hàng đã snapshot URL. */
@Getter
@Setter
@Entity
@Table(name = "product_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 200)
    private String alt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ProductImage(Product product, String url, int sortOrder) {
        this.product = product;
        this.url = url;
        this.sortOrder = sortOrder;
    }
}
