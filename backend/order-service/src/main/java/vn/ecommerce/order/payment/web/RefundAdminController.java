package vn.ecommerce.order.payment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.payment.internal.PaymentRefund;
import vn.ecommerce.order.payment.internal.RefundService;
import vn.ecommerce.order.payment.internal.RefundView;

@RestController
@RequestMapping("/api/admin/payments/refunds")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin: hoàn tiền")
class RefundAdminController {

    private final RefundService refundService;

    @GetMapping
    @Operation(summary = "Đơn đã hủy nhưng đã nhận tiền", description = "`status` PENDING (mặc định, cũ nhất trước) | DONE.")
    PageResponse<RefundView> list(@RequestParam(defaultValue = "PENDING") PaymentRefund.RefundStatus status,
                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return refundService.list(status, page, size);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đánh dấu đã hoàn tiền (chỉ ADMIN)", description = "Hoàn ở trang quản trị của cổng trước, rồi nhập mã giao dịch hoàn. "
            + "Gửi `version` đã đọc; 409 nếu lệch hoặc đã hoàn. Ghi admin_audit_log.")
    RefundView complete(@PathVariable UUID id, @Valid @RequestBody CompleteCommand command) {
        String note = command.note() == null || command.note().isBlank() ? null : command.note().trim();
        return refundService.complete(id, command.version(), command.reference().trim(), note);
    }

    record CompleteCommand(@NotNull(message = "{validation.required}") Long version,
                           @NotBlank(message = "{validation.required}") @Size(max = 100, message = "{validation.max-length}") String reference,
                           @Size(max = 255, message = "{validation.max-length}") String note) {
    }
}
