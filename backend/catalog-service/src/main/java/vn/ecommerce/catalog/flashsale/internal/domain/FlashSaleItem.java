package vn.ecommerce.catalog.flashsale.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/**
 * Một SKU trong chương trình. {@code sold} chỉ đổi bằng câu update nguyên tử của repository (nhiều đơn chốt cùng lúc),
 * không sửa qua entity.
 *
 * @see FlashSaleItemRepository#incrementSold
 */
@Getter
@Entity
@Table(name = "flash_sale_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashSaleItem extends AuditableEntity {

    @Column(name = "flash_sale_id", nullable = false)
    private UUID flashSaleId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "sale_price", nullable = false)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private int quota;

    @Column(nullable = false)
    private int sold;

    /** 0 = không giới hạn theo khách. */
    @Column(name = "per_customer_limit", nullable = false)
    private int perCustomerLimit;

    public FlashSaleItem(UUID flashSaleId, UUID skuId, BigDecimal salePrice, int quota, int perCustomerLimit) {
        this.flashSaleId = flashSaleId;
        this.skuId = skuId;
        this.salePrice = salePrice;
        this.quota = quota;
        this.perCustomerLimit = perCustomerLimit;
    }
}
