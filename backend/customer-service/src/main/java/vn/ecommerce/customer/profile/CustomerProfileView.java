package vn.ecommerce.customer.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Hồ sơ khách hàng trả cho storefront/admin. {@code id} = {@code sub} của Keycloak. */
public record CustomerProfileView(UUID id, String email, String fullName, String phone, LocalDate birthday, String gender,
                                  String status, Instant createdAt, long version) {
}
