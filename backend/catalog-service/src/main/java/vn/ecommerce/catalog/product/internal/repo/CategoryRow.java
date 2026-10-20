package vn.ecommerce.catalog.product.internal.repo;

import java.util.UUID;

/** Projection phẳng của danh mục để dựng cây trong bộ nhớ (một query cho cả cây). */
public record CategoryRow(UUID id, UUID parentId, String name, String slug, int sortOrder) {
}
