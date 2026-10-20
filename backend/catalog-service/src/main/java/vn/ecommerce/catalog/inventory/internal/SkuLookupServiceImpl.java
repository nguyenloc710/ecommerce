package vn.ecommerce.catalog.inventory.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.inventory.internal.domain.InventoryItem;
import vn.ecommerce.catalog.inventory.internal.dto.AvailabilityView;
import vn.ecommerce.catalog.inventory.internal.dto.SkuInfo;
import vn.ecommerce.catalog.inventory.internal.repo.InventoryItemRepository;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.common.jpa.InBatches;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SkuLookupServiceImpl implements SkuLookupService {

    private final VariantLookupApi variantLookupApi;
    private final InventoryItemRepository itemRepository;

    @Override
    public List<SkuInfo> lookup(Collection<UUID> skuIds) {
        Map<UUID, InventoryItem> items = itemsBySku(skuIds);
        return variantLookupApi.findVariants(skuIds).stream()
                .map(v -> new SkuInfo(v.skuId(), v.productId(), v.productSlug(), v.productName(), v.variantLabel(),
                        v.skuCode(), v.price(), v.compareAtPrice(), v.imageUrl(),
                        available(items.get(v.skuId())), v.sellable()))
                .toList();
    }

    @Override
    public List<AvailabilityView> availability(Collection<UUID> skuIds) {
        Map<UUID, InventoryItem> items = itemsBySku(skuIds);
        Map<UUID, VariantInfo> sellable = variantLookupApi.findVariants(skuIds).stream()
                .collect(Collectors.toMap(VariantInfo::skuId, Function.identity()));
        return skuIds.stream().distinct().map(skuId -> {
            InventoryItem item = items.get(skuId);
            VariantInfo info = sellable.get(skuId);
            int available = info != null && info.sellable() ? available(item) : 0;
            boolean low = item != null && available > 0 && available <= item.getLowStockThreshold();
            // Chỉ lộ số còn lại khi sắp hết để tạo cảm giác khan hiếm đúng chỗ, không lộ tồn kho thật
            return new AvailabilityView(skuId, available > 0, low, low ? available : null);
        }).toList();
    }

    private Map<UUID, InventoryItem> itemsBySku(Collection<UUID> skuIds) {
        return InBatches.query(skuIds, itemRepository::findBySkuIdIn).stream()
                .collect(Collectors.toMap(InventoryItem::getSkuId, Function.identity()));
    }

    private static int available(InventoryItem item) {
        return item == null ? 0 : Math.max(0, item.available());
    }
}
