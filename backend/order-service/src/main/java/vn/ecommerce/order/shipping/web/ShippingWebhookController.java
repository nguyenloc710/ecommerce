package vn.ecommerce.order.shipping.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.order.shipping.internal.ShippingService;

/** Webhook không có JWT (public-paths); xác thực bằng chữ ký HMAC trên body thô. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Webhook vận chuyển")
class ShippingWebhookController {

    static final String SIGNATURE_HEADER = "X-Signature";

    private final ShippingService shippingService;

    @PostMapping(value = "/webhooks/shipping/mock", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hãng giả báo trạng thái vận đơn", description = "Body `{trackingCode, status}`; header `X-Signature` = HMAC-SHA256(secret, body).")
    void mock(@RequestHeader(SIGNATURE_HEADER) String signature, @RequestBody String rawBody) {
        shippingService.handleMockWebhook(signature, rawBody);
    }
}
