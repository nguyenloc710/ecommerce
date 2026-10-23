package vn.ecommerce.catalog.flashsale.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CurrentFlashSale;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleQueryService;

@RestController
@RequestMapping("/api/flash-sales")
@RequiredArgsConstructor
@Tag(name = "Flash sale")
class FlashSaleController {

    private final FlashSaleQueryService queryService;

    @GetMapping("/current")
    @Operation(summary = "Chương trình đang chạy (công khai)", description = "Rỗng nếu không có; sắp kết thúc trước.")
    List<CurrentFlashSale> current() {
        return queryService.current();
    }
}
