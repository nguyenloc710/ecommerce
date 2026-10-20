package vn.ecommerce.catalog.product.internal.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/** Tạo/sửa sản phẩm (admin). Chuyển sang ACTIVE lần đầu sẽ set {@code published_at}. */
public record ProductUpsertCommand(
        @NotNull(message = "{validation.required}") UUID categoryId,
        UUID brandId,
        @NotBlank(message = "{validation.required}") @Size(max = 200, message = "{validation.max-length}") String name,
        @Size(max = 160, message = "{validation.max-length}") String slug,
        String description,
        Map<String, Object> attributes,
        @Pattern(regexp = "FIXED|GOLD_RATE", message = "{validation.enum}") String pricingType,
        @Pattern(regexp = "DRAFT|ACTIVE|HIDDEN", message = "{validation.enum}") String status,
        @Size(max = 200, message = "{validation.max-length}") String seoTitle,
        @Size(max = 300, message = "{validation.max-length}") String seoDescription,
        Long version) {
}
