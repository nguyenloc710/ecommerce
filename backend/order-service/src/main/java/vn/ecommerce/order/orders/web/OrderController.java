package vn.ecommerce.order.orders.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.orders.OrderSummaryView;
import vn.ecommerce.order.orders.OrderView;
import vn.ecommerce.order.orders.internal.OrderService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Đơn hàng của tôi")
class OrderController {

    private final OrderService orderService;

    @GetMapping("/me")
    @Operation(summary = "Danh sách đơn của tôi", description = "Mới nhất trước.")
    PageResponse<OrderSummaryView> mine(@RequestParam(defaultValue = "0") @Min(0) int page,
                                        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return orderService.listMine(page, size);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Chi tiết đơn của tôi", description = "404 nếu đơn không thuộc khách.")
    OrderView get(@PathVariable String code) {
        return orderService.getMine(code);
    }

    @PostMapping("/{code}/cancel")
    @Operation(summary = "Hủy đơn", description = "Chỉ khi đơn chưa đóng gói (PENDING/AWAITING_PAYMENT/CONFIRMED); 409 nếu muộn hơn.")
    OrderView cancel(@PathVariable String code, @Valid @RequestBody(required = false) CancelCommand command) {
        return orderService.cancelMine(code, command == null ? null : command.reason());
    }

    record CancelCommand(@Size(max = 255, message = "{validation.max-length}") String reason) {
    }
}
