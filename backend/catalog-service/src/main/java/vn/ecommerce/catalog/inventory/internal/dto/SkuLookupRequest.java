package vn.ecommerce.catalog.inventory.internal.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SkuLookupRequest(
        @NotEmpty(message = "{validation.required}") @Size(max = 200, message = "{validation.max-length}") List<UUID> skuIds) {
}
