package vn.ecommerce.order.shipping.internal;

import vn.ecommerce.order.shipping.ShippingApi;

public interface ShippingService extends ShippingApi {

    /**
     * Webhook của hãng giả: body JSON {@code {"trackingCode","status"}}, chữ ký HMAC-SHA256 trong header.
     *
     * @throws vn.ecommerce.common.web.error.ForbiddenException {@code shipping.invalid-signature}
     */
    void handleMockWebhook(String signature, String rawBody);
}
