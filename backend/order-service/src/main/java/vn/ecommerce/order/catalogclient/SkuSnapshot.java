package vn.ecommerce.order.catalogclient;

import java.math.BigDecimal;
import java.util.UUID;

/** Bản sao {@code SkuInfo} của catalog (hợp đồng {@code POST /internal/skus/lookup}). */
public record SkuSnapshot(UUID skuId, UUID productId, String productSlug, String productName, String variantLabel,
                          String skuCode, BigDecimal price, BigDecimal compareAtPrice, String imageUrl,
                          int available, boolean sellable) {
}
