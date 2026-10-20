package vn.ecommerce.catalog.product.internal.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Một ảnh trong danh sách thay thế toàn bộ; thứ tự trong mảng = {@code sort_order}. {@code id} null = ảnh mới. */
public record ImageItemCommand(
        UUID id,
        UUID variantId,
        @NotBlank(message = "{validation.required}") @Size(max = 500, message = "{validation.max-length}") String url,
        @Size(max = 200, message = "{validation.max-length}") String alt) {
}
