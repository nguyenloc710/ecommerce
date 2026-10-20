package vn.ecommerce.order.catalogclient;

import org.springframework.http.HttpStatus;
import vn.ecommerce.common.web.error.ApiException;

/** catalog-service không trả lời hoặc lỗi 5xx: khách thấy 502 và thử lại, không tạo đơn dở dang. */
public class CatalogUnavailableException extends ApiException {

    public static final String KEY = "catalog.unavailable";

    public CatalogUnavailableException() {
        super(HttpStatus.BAD_GATEWAY, KEY);
    }
}
