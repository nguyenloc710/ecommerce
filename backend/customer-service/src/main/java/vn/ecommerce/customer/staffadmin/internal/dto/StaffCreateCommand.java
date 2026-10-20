package vn.ecommerce.customer.staffadmin.internal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mật khẩu tạm được yêu cầu đổi ở lần đăng nhập đầu. */
public record StaffCreateCommand(
        @NotBlank(message = "{validation.required}") @Email(message = "{validation.email}")
        @Size(max = 255, message = "{validation.max-length}") String email,
        @NotBlank(message = "{validation.required}") @Size(max = 150, message = "{validation.max-length}") String fullName,
        @NotBlank(message = "{validation.required}") @Size(min = 8, max = 72, message = "{validation.password-length}")
        String temporaryPassword) {
}
