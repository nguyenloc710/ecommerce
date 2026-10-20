package vn.ecommerce.customer.staffadmin.internal.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.customer.staffadmin.internal.StaffAdminErrorKeys;

/** Gọi Keycloak Admin API bằng RestClient đã gắn token client credentials (xem {@code StaffAdminModuleConfig}). */
@Slf4j
@Component
class KeycloakAdminRestClient implements KeycloakAdminGateway {

    private static final ParameterizedTypeReference<List<KeycloakUser>> USER_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;
    private final KeycloakAdminProperties properties;

    KeycloakAdminRestClient(RestClient keycloakAdminHttpClient, KeycloakAdminProperties properties) {
        this.client = keycloakAdminHttpClient;
        this.properties = properties;
    }

    @Override
    public Optional<KeycloakUser> findUser(String userId) {
        try {
            return Optional.ofNullable(client.get().uri("/users/{id}", userId).retrieve().body(KeycloakUser.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw unavailable("findUser", e);
        }
    }

    @Override
    public void setEnabled(String userId, boolean enabled) {
        try {
            // PUT user chỉ với field cần đổi: Keycloak giữ nguyên các field null
            client.put().uri("/users/{id}", userId).body(Map.of("enabled", enabled)).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException(StaffAdminErrorKeys.USER_NOT_FOUND);
        } catch (RestClientException e) {
            throw unavailable("setEnabled", e);
        }
    }

    @Override
    public void logoutAllSessions(String userId) {
        try {
            client.post().uri("/users/{id}/logout", userId).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException(StaffAdminErrorKeys.USER_NOT_FOUND);
        } catch (RestClientException e) {
            throw unavailable("logoutAllSessions", e);
        }
    }

    @Override
    public String createStaff(String email, String fullName, String temporaryPassword) {
        Names names = Names.split(fullName);
        Map<String, Object> body = Map.of(
                "username", email,
                "email", email,
                "emailVerified", true,
                "enabled", true,
                "firstName", names.first(),
                "lastName", names.last(),
                "groups", List.of("/" + properties.staffGroup()),
                "requiredActions", List.of("UPDATE_PASSWORD"),
                "credentials", List.of(Map.of("type", "password", "value", temporaryPassword, "temporary", true)));
        try {
            ResponseEntity<Void> response = client.post().uri("/users").body(body).retrieve().toBodilessEntity();
            URI location = response.getHeaders().getLocation();
            if (response.getStatusCode() != HttpStatus.CREATED || location == null) {
                throw new KeycloakUnavailableException(StaffAdminErrorKeys.KEYCLOAK_UNAVAILABLE);
            }
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictException(StaffAdminErrorKeys.EMAIL_TAKEN, email);
        } catch (RestClientException e) {
            throw unavailable("createStaff", e);
        }
    }

    @Override
    public List<KeycloakUser> listStaff(int first, int max) {
        try {
            GroupRef group = client.get().uri("/group-by-path/{path}", properties.staffGroup()).retrieve().body(GroupRef.class);
            if (group == null) {
                return List.of();
            }
            List<KeycloakUser> members = client.get()
                    .uri("/groups/{id}/members?first={first}&max={max}", group.id(), first, max)
                    .retrieve().body(USER_LIST);
            return members == null ? List.of() : members;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Group nhân viên '{}' chưa có trong realm", properties.staffGroup());
            return List.of();
        } catch (RestClientException e) {
            throw unavailable("listStaff", e);
        }
    }

    private KeycloakUnavailableException unavailable(String operation, RestClientException cause) {
        log.error("Keycloak Admin API lỗi ở {}: {}", operation, cause.getMessage());
        return new KeycloakUnavailableException(StaffAdminErrorKeys.KEYCLOAK_UNAVAILABLE);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GroupRef(String id) {
    }

    /** Tên Việt: từ cuối là tên (lastName theo Keycloak), phần còn lại là họ đệm; Keycloak ghép lại thành claim {@code name}. */
    record Names(String first, String last) {

        static Names split(String fullName) {
            String trimmed = fullName.trim();
            int cut = trimmed.lastIndexOf(' ');
            return cut < 0 ? new Names("", trimmed) : new Names(trimmed.substring(0, cut), trimmed.substring(cut + 1));
        }
    }
}
