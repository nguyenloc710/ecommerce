package vn.ecommerce.order.customerclient;

import org.springframework.http.HttpStatus;
import vn.ecommerce.common.web.error.ApiException;

public class CustomerUnavailableException extends ApiException {

    public static final String KEY = "customer.unavailable";

    public CustomerUnavailableException() {
        super(HttpStatus.BAD_GATEWAY, KEY);
    }
}
