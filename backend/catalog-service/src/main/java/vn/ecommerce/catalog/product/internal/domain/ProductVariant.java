package vn.ecommerce.catalog.product.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import vn.ecommerce.common.jpa.SoftDeletableEntity;

/**
 * Biến thể bán được (SKU): tổ hợp option (size, màu...) + giá. Tồn kho, giỏ hàng, đơn hàng tham chiếu id này.
 */
@Getter
@Setter
@Entity
@Table(name = "product_variant")
@Audited
@AuditOverride(forClass = SoftDeletableEntity.class)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku_code", nullable = false, length = 64)
    private String skuCode;

    /** Ví dụ {@code {"size":"M","color":"Đen"}}; key theo {@code attribute_template} của danh mục. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_values", nullable = false)
    private Map<String, String> optionValues = new LinkedHashMap<>();

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 19, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "weight_gram")
    private Integer weightGram;

    @Column(length = 64)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VariantStatus status = VariantStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Chỉ dùng khi sản phẩm GOLD_RATE (B-09): loại vàng, trọng lượng vàng (gam) và phí gia công. */
    @Enumerated(EnumType.STRING)
    @Column(name = "gold_karat", length = 8)
    private GoldKarat goldKarat;

    @Column(name = "gold_weight", precision = 10, scale = 3)
    private BigDecimal goldWeight;

    @Column(name = "making_fee", precision = 19, scale = 2)
    private BigDecimal makingFee;

    public boolean hasGoldPricing() {
        return goldKarat != null && goldWeight != null && makingFee != null;
    }

    public ProductVariant(Product product, String skuCode, BigDecimal price) {
        this.product = product;
        this.skuCode = skuCode;
        this.price = price;
    }
}
