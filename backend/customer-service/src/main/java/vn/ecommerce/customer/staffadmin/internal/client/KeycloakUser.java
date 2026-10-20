package vn.ecommerce.customer.staffadmin.internal.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Phần cần dùng của UserRepresentation (Keycloak Admin API). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUser(String id, String username, String email, String firstName, String lastName, boolean enabled,
                           Long createdTimestamp) {

    public String fullName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }
}
