package vn.ecommerce.customer.staffadmin.internal.dto;

import java.time.Instant;

/** Tài khoản nhân viên (nguồn: Keycloak). */
public record StaffView(String id, String email, String fullName, boolean enabled, Instant createdAt) {
}
