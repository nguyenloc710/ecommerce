package vn.ecommerce.catalog.inventory.internal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Admin đặt số tồn tuyệt đối sau kiểm kê/nhập hàng. {@code version} null được chấp nhận khi SKU chưa có bản ghi tồn kho.
 */
public record InventoryUpdateCommand(
        @NotNull(message = "{validation.required}") @Min(value = 0, message = "{validation.min}") Integer onHand,
        @Min(value = 0, message = "{validation.min}") Integer lowStockThreshold,
        Long version) {
}
