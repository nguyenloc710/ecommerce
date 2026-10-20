package vn.ecommerce.catalog.inventory.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.inventory.internal.SkuLookupService;
import vn.ecommerce.catalog.inventory.internal.dto.AvailabilityView;

/** Công khai (GET) cho PDP: còn hàng / sắp hết, không lộ số tồn khi còn nhiều. */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tồn kho (công khai)")
class AvailabilityController {

    private final SkuLookupService skuLookupService;

    @GetMapping("/availability")
    @Operation(summary = "Tình trạng còn hàng của các SKU", description = "`skuIds` tối đa 100, phân tách bằng dấu phẩy.")
    List<AvailabilityView> availability(@RequestParam @Size(min = 1, max = 100, message = "{validation.max-length}") List<UUID> skuIds) {
        return skuLookupService.availability(skuIds);
    }
}
