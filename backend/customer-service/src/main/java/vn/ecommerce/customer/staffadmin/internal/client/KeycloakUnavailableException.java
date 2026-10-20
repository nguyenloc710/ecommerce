package vn.ecommerce.customer.staffadmin.internal.client;

import org.springframework.http.HttpStatus;
import vn.ecommerce.common.web.error.ApiException;

/** Keycloak không trả lời hoặc lỗi 5xx: trả 502 để admin thử lại, không ghi dở dữ liệu. */
public class KeycloakUnavailableException extends ApiException {

    public KeycloakUnavailableException(String messageKey) {
        super(HttpStatus.BAD_GATEWAY, messageKey);
    }
}
