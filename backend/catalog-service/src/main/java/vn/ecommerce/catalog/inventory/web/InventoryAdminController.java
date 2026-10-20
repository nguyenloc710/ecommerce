package vn.ecommerce.catalog.inventory.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.inventory.internal.StockAdminService;
import vn.ecommerce.catalog.inventory.internal.dto.InventoryRow;
import vn.ecommerce.catalog.inventory.internal.dto.InventoryUpdateCommand;
import vn.ecommerce.catalog.product.PageView;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin · Tồn kho")
class InventoryAdminController {

    private final StockAdminService stockAdminService;

    @GetMapping
    @Operation(summary = "Bảng tồn kho", description = "`q` tìm theo mã SKU/tên sản phẩm; `lowStock=true` chỉ SKU sắp hết. Sắp theo số còn bán tăng dần.")
    PageView<InventoryRow> page(@RequestParam(required = false) String q,
                                @RequestParam(defaultValue = "false") boolean lowStock,
                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return stockAdminService.page(q, lowStock, page, size);
    }

    @PutMapping("/{skuId}")
    @Operation(summary = "Đặt số tồn tuyệt đối cho SKU", description = "400 `inventory.on-hand-below-reserved` nếu thấp hơn số đang giữ; 409 nếu `version` lệch.")
    InventoryRow update(@PathVariable UUID skuId, @Valid @RequestBody InventoryUpdateCommand command) {
        return stockAdminService.update(skuId, command);
    }
}
