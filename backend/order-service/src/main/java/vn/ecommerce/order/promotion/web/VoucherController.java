package vn.ecommerce.order.promotion.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.order.promotion.DiscountQuote;
import vn.ecommerce.order.promotion.internal.PromotionService;
import vn.ecommerce.order.promotion.internal.VoucherValidateCommand;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mã giảm giá")
class VoucherController {

    private final PromotionService promotionService;

    @PostMapping("/api/vouchers/validate")
    @Operation(summary = "Kiểm tra mã giảm giá", description = "400 `promotion.*` kèm lý do khi không áp dụng được.")
    DiscountQuote validate(@Valid @RequestBody VoucherValidateCommand command) {
        return promotionService.validateForCurrent(command);
    }
}
