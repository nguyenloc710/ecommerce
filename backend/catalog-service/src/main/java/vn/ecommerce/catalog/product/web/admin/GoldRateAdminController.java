package vn.ecommerce.catalog.product.web.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.GoldRateView;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.SetRateCommand;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.SetRateResult;
import vn.ecommerce.catalog.product.internal.gold.GoldRateService;

@RestController
@RequestMapping("/api/admin/catalog/gold-rates")
@RequiredArgsConstructor
@Tag(name = "Quản trị giá vàng")
class GoldRateAdminController {

    private final GoldRateService goldRateService;

    @GetMapping
    @Operation(summary = "Lịch sử nhập giá vàng, mới nhất trước", description = "`karat` = K24 | K18 | K14 | K10 (bỏ trống = mọi loại).")
    PageView<GoldRateView> history(@RequestParam(required = false) String karat, @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return goldRateService.history(karat, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Nhập giá vàng mới", description = "Hiệu lực ngay; tính lại giá mọi variant GOLD_RATE dùng loại vàng này. "
            + "Ghi admin_audit_log.")
    SetRateResult setRate(@Valid @RequestBody SetRateCommand command) {
        return goldRateService.setRate(command);
    }
}
