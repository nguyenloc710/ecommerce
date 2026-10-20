package vn.ecommerce.catalog.inventory.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.inventory.internal.SkuLookupService;
import vn.ecommerce.catalog.inventory.internal.dto.SkuInfo;
import vn.ecommerce.catalog.inventory.internal.dto.SkuLookupRequest;

/** Chỉ service account có role INTERNAL (order-service) gọi; không đi qua gateway. */
@RestController
@RequestMapping("/internal/skus")
@RequiredArgsConstructor
@Tag(name = "Internal · SKU")
class InternalSkuController {

    private final SkuLookupService skuLookupService;

    @PostMapping("/lookup")
    @Operation(summary = "Tra cứu SKU theo lô (giá, tên, ảnh, tồn kho)", description = "SKU không tồn tại bị bỏ qua trong kết quả; SKU ẩn trả sellable=false.")
    List<SkuInfo> lookup(@Valid @RequestBody SkuLookupRequest request) {
        return skuLookupService.lookup(request.skuIds());
    }
}
