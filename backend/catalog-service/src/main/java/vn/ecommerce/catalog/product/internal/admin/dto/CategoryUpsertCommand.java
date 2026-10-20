package vn.ecommerce.catalog.product.internal.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/**
 * Tạo/sửa danh mục (admin). {@code slug} trống thì sinh từ tên. {@code version} bắt buộc khi sửa (optimistic lock).
 */
public record CategoryUpsertCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 120, message = "{validation.max-length}") String name,
        @Size(max = 140, message = "{validation.max-length}") String slug,
        UUID parentId,
        @Size(max = 2000, message = "{validation.max-length}") String description,
        Map<String, Object> attributeTemplate,
        Integer sortOrder,
        @Pattern(regexp = "ACTIVE|HIDDEN", message = "{validation.enum}") String status,
        Long version) {
}
