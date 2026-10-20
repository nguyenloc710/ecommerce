package vn.ecommerce.order.promotion.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.promotion.VoucherView;
import vn.ecommerce.order.promotion.internal.PromotionService;
import vn.ecommerce.order.promotion.internal.VoucherUpsertCommand;

/** B-11: STAFF không sửa voucher; chỉ ADMIN. */
@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: mã giảm giá")
class VoucherAdminController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(summary = "Danh sách voucher")
    PageResponse<VoucherView> search(@RequestParam(required = false) String q,
                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return promotionService.search(q, page, size);
    }

    @GetMapping("/{id}")
    VoucherView get(@PathVariable UUID id) {
        return promotionService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo voucher", description = "Mã được chuẩn hóa chữ hoa; 409 nếu trùng.")
    VoucherView create(@Valid @RequestBody VoucherUpsertCommand command) {
        return promotionService.create(command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa voucher", description = "Không đổi mã; bắt buộc `version`.")
    VoucherView update(@PathVariable UUID id, @Valid @RequestBody VoucherUpsertCommand command) {
        return promotionService.update(id, command);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa voucher (xóa mềm)")
    void delete(@PathVariable UUID id) {
        promotionService.delete(id);
    }
}
