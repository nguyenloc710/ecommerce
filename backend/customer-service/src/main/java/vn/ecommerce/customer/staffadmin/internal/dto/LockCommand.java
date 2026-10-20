package vn.ecommerce.customer.staffadmin.internal.dto;

import jakarta.validation.constraints.Size;

/** Lý do khóa, lưu vào audit log. */
public record LockCommand(@Size(max = 255, message = "{validation.max-length}") String reason) {
}
