package vn.ecommerce.customer.address.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Tạo/sửa địa chỉ. {@code version} bắt buộc khi sửa, bỏ qua khi tạo. */
public record AddressUpsertCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 150, message = "{validation.max-length}") String recipientName,
        @NotBlank(message = "{validation.required}") @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "{validation.phone}") String phone,
        @NotBlank(message = "{validation.required}") @Size(max = 10, message = "{validation.max-length}") String provinceCode,
        @NotBlank(message = "{validation.required}") @Size(max = 10, message = "{validation.max-length}") String wardCode,
        @NotBlank(message = "{validation.required}") @Size(max = 255, message = "{validation.max-length}") String line1,
        @Size(max = 255, message = "{validation.max-length}") String note,
        // Boolean (không phải boolean): Jackson 3 từ chối body thiếu field kiểu nguyên thủy; null = false
        Boolean isDefault,
        Long version) {
}
