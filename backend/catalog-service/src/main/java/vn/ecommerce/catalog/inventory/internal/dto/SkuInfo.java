package vn.ecommerce.catalog.inventory.internal.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Hợp đồng {@code POST /internal/skus/lookup} cho order-service: dữ liệu để snapshot vào giỏ/đơn + tồn kho hiện tại.
 *
 * @param available số có thể bán ngay (on_hand - reserved); 0 nếu chưa có bản ghi tồn kho
 * @param sellable  SKU và sản phẩm đang ACTIVE
 */
public record SkuInfo(UUID skuId, UUID productId, String productSlug, String productName, String variantLabel,
                      String skuCode, BigDecimal price, BigDecimal compareAtPrice, String imageUrl,
                      int available, boolean sellable) {
}
