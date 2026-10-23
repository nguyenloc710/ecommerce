package vn.ecommerce.catalog.flashsale.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.flashsale.internal.domain.ActiveItem;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleItemRepository;
import vn.ecommerce.catalog.product.PriceAdjuster;
import vn.ecommerce.common.jpa.InBatches;

/**
 * Giá flash sale cho SKU đang trong chương trình chạy và còn suất theo DB (đã trừ suất đơn chưa chốt đang giữ).
 * Không phụ thuộc module product ngoài SPI này: product inject bean này, nên không được inject ngược API của product.
 */
@Component
@RequiredArgsConstructor
class FlashSalePriceAdjuster implements PriceAdjuster {

    private final FlashSaleItemRepository items;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, AdjustedPrice> currentPrices(Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        Instant now = Instant.now();
        return InBatches.query(skuIds, ids -> items.findActiveBySkuIdIn(ids, now)).stream()
                .filter(item -> item.remaining() > 0)
                .collect(Collectors.toMap(ActiveItem::skuId,
                        item -> new AdjustedPrice(item.salePrice(), FlashSaleConstants.PRICE_LABEL, item.endsAt()), (a, b) -> a));
    }
}
