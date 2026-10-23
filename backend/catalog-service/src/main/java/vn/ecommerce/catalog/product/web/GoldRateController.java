package vn.ecommerce.catalog.product.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.GoldRateView;
import vn.ecommerce.catalog.product.internal.gold.GoldRateService;

@RestController
@RequestMapping("/api/gold-rates")
@RequiredArgsConstructor
@Tag(name = "Giá vàng")
class GoldRateController {

    private final GoldRateService goldRateService;

    @GetMapping("/current")
    @Operation(summary = "Giá vàng hiện hành theo loại (công khai)")
    List<GoldRateView> current() {
        return goldRateService.current();
    }
}
