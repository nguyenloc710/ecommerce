package vn.ecommerce.catalog.product.internal.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record BrandAdminView(UUID id, String name, String slug, String logoUrl, String description,
                             Instant updatedAt, String updatedBy, long version) {
}
