package vn.ecommerce.catalog.inventory.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.inventory.internal.ReservationService;
import vn.ecommerce.catalog.inventory.internal.dto.ReservationResult;
import vn.ecommerce.catalog.inventory.internal.dto.ReserveRequest;

@RestController
@RequestMapping("/internal/reservations")
@RequiredArgsConstructor
@Tag(name = "Internal · Giữ hàng")
class InternalReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Giữ hàng cho đơn (idempotent theo orderId)",
            description = "409 `inventory.insufficient` khi thiếu hàng (không giữ dòng nào), `inventory.sku-not-sellable` khi SKU không bán, `inventory.busy` khi tranh chấp quá nhiều.")
    ReservationResult reserve(@Valid @RequestBody ReserveRequest request) {
        return reservationService.reserve(request.orderId(), request.customerId(), request.lines());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Trạng thái giữ hàng của đơn")
    ReservationResult status(@PathVariable UUID orderId) {
        return reservationService.status(orderId);
    }

    @PostMapping("/{orderId}/commit")
    @Operation(summary = "Chốt hàng (trừ tồn) khi đơn xác nhận; idempotent")
    ReservationResult commit(@PathVariable UUID orderId) {
        return reservationService.commit(orderId);
    }

    @PostMapping("/{orderId}/release")
    @Operation(summary = "Nhả hàng khi đơn hủy; idempotent")
    ReservationResult release(@PathVariable UUID orderId) {
        return reservationService.release(orderId);
    }
}
