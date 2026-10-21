package vn.ecommerce.order.payment.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.ecommerce.common.web.error.ApiException;
import vn.ecommerce.common.web.error.ForbiddenException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.order.payment.PaymentMethod;

/**
 * Chuyển kết quả xử lý IPN sang mã trả lời VNPay. VNPay luôn cần HTTP 200 + {@code RspCode}; mã khác "00"/"02" thì
 * VNPay gọi lại. Nghiệp vụ dùng chung khung webhook của {@link PaymentService} (verify → idempotency → áp dụng).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VnpayIpnHandler {

    private final PaymentService paymentService;

    public IpnReply handle(String rawQuery) {
        String signature = VnpaySigner.parseQuery(rawQuery).get(VnpaySigner.SECURE_HASH);
        try {
            paymentService.handleWebhook(PaymentMethod.VNPAY, signature, rawQuery);
            // Gồm cả trường hợp đã xử lý trước đó (idempotent) và số tiền lệch (đã ghi thất bại): không cần VNPay gọi lại
            return IpnReply.CONFIRMED;
        } catch (ForbiddenException e) {
            return IpnReply.INVALID_SIGNATURE;
        } catch (NotFoundException e) {
            return IpnReply.ORDER_NOT_FOUND;
        } catch (ApiException e) {
            log.warn("IPN VNPay lỗi nghiệp vụ: {}", e.getMessageKey());
            return IpnReply.UNKNOWN_ERROR;
        }
    }

    /** Hợp đồng trả lời IPN của VNPay (tên trường viết hoa theo tài liệu VNPay). */
    public enum IpnReply {
        CONFIRMED("00", "Confirm Success"),
        ORDER_NOT_FOUND("01", "Order not found"),
        INVALID_SIGNATURE("97", "Invalid Checksum"),
        UNKNOWN_ERROR("99", "Unknown error");

        private final String code;
        private final String message;

        IpnReply(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String code() {
            return code;
        }

        public String message() {
            return message;
        }
    }
}
