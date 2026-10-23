package vn.ecommerce.catalog.flashsale.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CurrentFlashSale;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CurrentItem;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSale;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleHoldRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleItem;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleItemRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleStatus;
import vn.ecommerce.catalog.flashsale.internal.domain.ItemQuantity;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class FlashSaleQueryServiceImpl implements FlashSaleQueryService {

    private final FlashSaleRepository sales;
    private final FlashSaleItemRepository items;
    private final FlashSaleHoldRepository holds;
    private final VariantLookupApi variants;

    @Override
    public List<CurrentFlashSale> current() {
        Instant now = Instant.now();
        List<FlashSale> running = sales.findByStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByEndsAtAsc(FlashSaleStatus.ACTIVE, now, now);
        if (running.isEmpty()) {
            return List.of();
        }
        List<FlashSaleItem> all = items.findByFlashSaleIdIn(running.stream().map(FlashSale::getId).toList());
        if (all.isEmpty()) {
            return List.of();
        }
        Map<UUID, VariantInfo> skus = variants.findVariants(all.stream().map(FlashSaleItem::getSkuId).toList()).stream()
                .filter(VariantInfo::sellable).collect(Collectors.toMap(VariantInfo::skuId, Function.identity()));
        Map<UUID, Long> held = holds.sumHeld(all.stream().map(FlashSaleItem::getId).toList()).stream()
                .collect(Collectors.toMap(ItemQuantity::itemId, ItemQuantity::quantity));
        Map<UUID, List<FlashSaleItem>> bySale = all.stream().collect(Collectors.groupingBy(FlashSaleItem::getFlashSaleId));
        return running.stream().map(sale -> new CurrentFlashSale(sale.getId(), sale.getName(), sale.getStartsAt(), sale.getEndsAt(),
                        bySale.getOrDefault(sale.getId(), List.of()).stream().filter(i -> skus.containsKey(i.getSkuId()))
                                .map(i -> toItem(i, skus.get(i.getSkuId()), held.getOrDefault(i.getId(), 0L))).toList()))
                .filter(sale -> !sale.items().isEmpty()).toList();
    }

    private static CurrentItem toItem(FlashSaleItem item, VariantInfo sku, long held) {
        long claimed = Math.min(item.getQuota(), item.getSold() + held);
        return new CurrentItem(sku.skuId(), sku.productId(), sku.productSlug(), sku.productName(), sku.variantLabel(), sku.imageUrl(),
                compareAt(sku), item.getSalePrice(), item.getQuota(), (int) (claimed * 100 / item.getQuota()), claimed >= item.getQuota());
    }

    /** Giá gạch gốc nếu cao hơn giá niêm yết, không thì giá niêm yết (khớp {@code PriceAdjustments.compareAtWhenAdjusted}). */
    private static BigDecimal compareAt(VariantInfo sku) {
        return sku.compareAtPrice() != null && sku.compareAtPrice().compareTo(sku.listPrice()) > 0 ? sku.compareAtPrice() : sku.listPrice();
    }
}
