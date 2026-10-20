package vn.ecommerce.catalog.product.internal.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandUpsertCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 120, message = "{validation.max-length}") String name,
        @Size(max = 140, message = "{validation.max-length}") String slug,
        @Size(max = 500, message = "{validation.max-length}") String logoUrl,
        @Size(max = 2000, message = "{validation.max-length}") String description,
        Long version) {
}
