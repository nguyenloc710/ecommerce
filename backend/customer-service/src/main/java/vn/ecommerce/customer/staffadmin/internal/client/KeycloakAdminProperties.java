package vn.ecommerce.customer.staffadmin.internal.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kết nối Keycloak Admin API.
 *
 * @param baseUrl        gốc Keycloak, ví dụ {@code http://keycloak:8080}
 * @param realm          realm chứa người dùng của shop
 * @param staffGroup     tên group nhân viên (gán role STAFF)
 * @param registrationId client registration (client credentials) dùng để lấy token
 */
@ConfigurationProperties(prefix = "ecommerce.clients.keycloak-admin")
public record KeycloakAdminProperties(String baseUrl, String realm, String staffGroup, String registrationId) {

    public KeycloakAdminProperties {
        staffGroup = staffGroup == null || staffGroup.isBlank() ? "staff" : staffGroup;
        registrationId = registrationId == null || registrationId.isBlank() ? "keycloak-admin" : registrationId;
    }

    public String adminRealmUrl() {
        return baseUrl.replaceAll("/+$", "") + "/admin/realms/" + realm;
    }
}
