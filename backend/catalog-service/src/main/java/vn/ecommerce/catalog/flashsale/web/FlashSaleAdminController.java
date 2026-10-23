package vn.ecommerce.catalog.flashsale.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleAdminService;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CancelCommand;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CreateCommand;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.FlashSaleDetail;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.FlashSaleRow;
import vn.ecommerce.common.web.error.PageResponse;

@RestController
@RequestMapping("/api/admin/catalog/flash-sales")
@RequiredArgsConstructor
@Tag(name = "Quản trị flash sale")
class FlashSaleAdminController {

    private final FlashSaleAdminService adminService;

    @GetMapping
    @Operation(summary = "Danh sách chương trình, mới nhất trước", description = "`phase` = UPCOMING | RUNNING | ENDED | CANCELLED.")
    PageResponse<FlashSaleRow> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminService.list(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết chương trình kèm suất đã bán và đang giữ theo SKU")
    FlashSaleDetail detail(@PathVariable UUID id) {
        return adminService.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo chương trình", description = "Giá sale phải thấp hơn giá niêm yết; một SKU không nằm trong hai chương trình "
            + "trùng giờ. Ghi admin_audit_log.")
    FlashSaleDetail create(@Valid @RequestBody CreateCommand command) {
        return adminService.create(command);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Dừng chương trình", description = "Gửi `version` đã đọc; đơn đã giữ suất vẫn giữ giá. Ghi admin_audit_log.")
    FlashSaleDetail cancel(@PathVariable UUID id, @Valid @RequestBody CancelCommand command) {
        return adminService.cancel(id, command.version());
    }
}
