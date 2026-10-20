package vn.ecommerce.catalog.product.internal.admin.dto;

import java.time.Instant;
import java.util.UUID;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;

/** Một dòng trong bảng sản phẩm của admin (projection từ JPQL). */
public record ProductAdminRow(UUID id, String slug, String name, String categoryName, ProductStatus status,
                              Instant publishedAt, Instant updatedAt, long version) {
}
