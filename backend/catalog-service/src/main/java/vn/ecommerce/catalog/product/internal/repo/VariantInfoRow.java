package vn.ecommerce.catalog.product.internal.repo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;
import vn.ecommerce.catalog.product.internal.domain.VariantStatus;

public record VariantInfoRow(UUID skuId, UUID productId, String productSlug, String productName, String skuCode,
                             Map<String, String> optionValues, BigDecimal price, BigDecimal compareAtPrice,
                             VariantStatus variantStatus, ProductStatus productStatus) {
}
