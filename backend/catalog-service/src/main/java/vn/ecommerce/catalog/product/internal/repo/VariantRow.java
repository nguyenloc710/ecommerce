package vn.ecommerce.catalog.product.internal.repo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record VariantRow(UUID productId, UUID id, BigDecimal price, BigDecimal compareAtPrice, Map<String, String> optionValues) {
}
