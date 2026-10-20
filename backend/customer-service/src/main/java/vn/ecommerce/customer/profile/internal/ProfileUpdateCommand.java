package vn.ecommerce.customer.profile.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Khách tự sửa hồ sơ; email đổi ở Keycloak (Account Console), không đổi ở đây. */
public record ProfileUpdateCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 150, message = "{validation.max-length}") String fullName,
        @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "{validation.phone}") String phone,
        @Past(message = "{validation.date-past}") LocalDate birthday,
        @Pattern(regexp = "FEMALE|MALE|OTHER", message = "{validation.enum}") String gender,
        @NotNull(message = "{validation.required}") Long version) {
}
