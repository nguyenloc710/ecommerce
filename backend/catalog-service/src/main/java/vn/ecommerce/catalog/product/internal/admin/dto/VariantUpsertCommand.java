package vn.ecommerce.catalog.product.internal.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

public record VariantUpsertCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 64, message = "{validation.max-length}") String skuCode,
        Map<String, String> optionValues,
        @NotNull(message = "{validation.required}") @DecimalMin(value = "0", message = "{validation.min}") BigDecimal price,
        @DecimalMin(value = "0", message = "{validation.min}") BigDecimal compareAtPrice,
        @PositiveOrZero(message = "{validation.min}") Integer weightGram,
        @Size(max = 64, message = "{validation.max-length}") String barcode,
        @Pattern(regexp = "ACTIVE|HIDDEN", message = "{validation.enum}") String status,
        Integer sortOrder,
        Long version) {
}
