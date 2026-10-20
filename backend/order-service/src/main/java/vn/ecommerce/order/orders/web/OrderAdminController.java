package vn.ecommerce.order.orders.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
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
import vn.ecommerce.order.orders.OrderStatus;
import vn.ecommerce.order.orders.OrderSummaryView;
import vn.ecommerce.order.orders.OrderView;
import vn.ecommerce.order.orders.internal.OrderAdminService;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin: đơn hàng")
class OrderAdminController {

    private final OrderAdminService adminService;

    @GetMapping
    @Operation(summary = "Danh sách đơn", description = "`status` = tab trạng thái; `q` tìm theo mã, email, SĐT, tên người nhận.")
    PageResponse<OrderSummaryView> search(@RequestParam(required = false) OrderStatus status,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.search(status, q, page, size);
    }

    @GetMapping("/{code}")
    OrderView get(@PathVariable String code) {
        return adminService.get(code);
    }

    @PostMapping("/{code}/status")
    @Operation(summary = "Chuyển trạng thái", description = "Kiểm tra bảng chuyển (B-04); SHIPPING dùng `/ship`, CANCELLED dùng `/cancel`.")
    OrderView changeStatus(@PathVariable String code, @Valid @RequestBody StatusCommand command) {
        return adminService.changeStatus(code, command.to(), command.reason());
    }

    @PostMapping("/{code}/ship")
    @Operation(summary = "Tạo vận đơn và chuyển SHIPPING", description = "`carrier` trống = hãng khách đã chọn.")
    OrderView ship(@PathVariable String code, @Valid @RequestBody(required = false) ShipCommand command) {
        return adminService.ship(code, command == null ? null : command.carrier());
    }

    @PostMapping("/ship")
    @Operation(summary = "Tạo vận đơn hàng loạt", description = "Đơn lỗi được liệt kê trong `failed`, không ảnh hưởng đơn khác.")
    OrderAdminService.BulkShipResult bulkShip(@Valid @RequestBody BulkShipCommand command) {
        return adminService.bulkShip(command.codes(), command.carrier());
    }

    @PostMapping("/{code}/cancel")
    @Operation(summary = "Hủy đơn", description = "Được đến PACKING; từ SHIPPING dùng luồng đổi trả.")
    OrderView cancel(@PathVariable String code, @Valid @RequestBody CancelCommand command) {
        return adminService.cancel(code, command.reason());
    }

    record StatusCommand(@NotNull(message = "{validation.required}") OrderStatus to,
                         @Size(max = 255, message = "{validation.max-length}") String reason) {
    }

    record ShipCommand(@Size(max = 20, message = "{validation.max-length}") String carrier) {
    }

    record BulkShipCommand(@NotEmpty(message = "{validation.required}") @Size(max = 100, message = "{validation.max-length}") List<String> codes,
                           @Size(max = 20, message = "{validation.max-length}") String carrier) {
    }

    record CancelCommand(@NotNull(message = "{validation.required}") @Size(min = 1, max = 255, message = "{validation.max-length}") String reason) {
    }
}
