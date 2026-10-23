package vn.ecommerce.catalog.flashsale.internal;

import org.springframework.http.HttpStatus;
import vn.ecommerce.common.web.error.ApiException;

/** Valkey không trả lời: từ chối giữ suất (fail closed) thay vì bán vượt suất. */
class FlashSaleUnavailableException extends ApiException {

    FlashSaleUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, FlashSaleErrorKeys.UNAVAILABLE);
    }
}
