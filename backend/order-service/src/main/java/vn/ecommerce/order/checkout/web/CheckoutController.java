package vn.ecommerce.order.checkout.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.order.checkout.CheckoutQuoteView;
import vn.ecommerce.order.checkout.CheckoutResult;
import vn.ecommerce.order.checkout.internal.CheckoutRequest;
import vn.ecommerce.order.checkout.internal.CheckoutService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Thanh toán")
class CheckoutController {

    static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final CheckoutService checkoutService;

    @PostMapping("/api/checkout/quote")
    @Operation(summary = "Báo giá trước khi đặt", description = "Giá tra lại từ catalog; trả các lựa chọn giao hàng và giảm giá nếu có mã.")
    CheckoutQuoteView quote(@Valid @RequestBody CheckoutRequest request) {
        return checkoutService.quote(request);
    }

    @PostMapping("/api/orders")
    @Operation(summary = "Đặt hàng", description = "Bắt buộc header `Idempotency-Key` (UUID do FE sinh cho mỗi lần bấm Đặt hàng). "
            + "201 khi tạo mới, 200 khi key đã dùng (trả đơn cũ). COD xác nhận ngay; online trả `payUrl`.")
    ResponseEntity<CheckoutResult> place(@RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                                         @Valid @RequestBody CheckoutRequest request) {
        CheckoutResult result = checkoutService.placeOrder(request, idempotencyKey);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }
}
