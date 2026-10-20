package vn.ecommerce.order.payment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.internal.PaymentService;

/** Webhook cổng thanh toán: không JWT (public-paths), xác thực bằng chữ ký trên body thô. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Webhook thanh toán")
class PaymentWebhookController {

    static final String SIGNATURE_HEADER = "X-Signature";

    private final PaymentService paymentService;

    @PostMapping(value = "/webhooks/payments/{provider}", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cổng thanh toán báo kết quả", description = "`provider` = `mock` (VNPay/MoMo thêm ở E6). Header `X-Signature` = HMAC-SHA256(secret, body).")
    void receive(@PathVariable String provider, @RequestHeader(SIGNATURE_HEADER) String signature, @RequestBody String rawBody) {
        PaymentMethod method = PaymentMethod.fromSlug(provider).orElseThrow(() -> new NotFoundException("payment.method-not-supported", provider));
        paymentService.handleWebhook(method, signature, rawBody);
    }
}
