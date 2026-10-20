package vn.ecommerce.catalog.product.internal.repo;

import java.util.UUID;

public record ImageRow(UUID id, UUID productId, UUID variantId, String url, String alt, int sortOrder) {
}
