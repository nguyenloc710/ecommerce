package vn.ecommerce.catalog.product.internal.admin.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CategoryAdminView(UUID id, UUID parentId, String name, String slug, String description,
                                Map<String, Object> attributeTemplate, int sortOrder, String status,
                                Instant updatedAt, String updatedBy, long version) {
}
