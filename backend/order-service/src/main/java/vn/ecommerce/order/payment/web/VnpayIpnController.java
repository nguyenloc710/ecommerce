package vn.ecommerce.order.payment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.order.payment.internal.VnpayIpnHandler;

/**
 * IPN của VNPay: GET kèm tham số {@code vnp_*} và chữ ký trên query string, không JWT (public-paths).
 * Luôn trả 200 với {@code RspCode}/{@code Message} theo tài liệu VNPay.
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ecommerce.payment.vnpay", name = "enabled", havingValue = "true")
@Tag(name = "Webhook thanh toán")
class VnpayIpnController {

    private final VnpayIpnHandler handler;

    @GetMapping("/webhooks/payments/vnpay")
    @Operation(summary = "VNPay báo kết quả giao dịch (IPN)", description = "Chữ ký `vnp_SecureHash` = HMAC-SHA512(hashSecret, tham số vnp_* đã sắp xếp).")
    Map<String, String> ipn(HttpServletRequest request) {
        VnpayIpnHandler.IpnReply reply = handler.handle(request.getQueryString());
        return Map.of("RspCode", reply.code(), "Message", reply.message());
    }
}
