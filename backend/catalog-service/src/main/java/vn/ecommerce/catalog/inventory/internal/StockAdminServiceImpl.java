package vn.ecommerce.catalog.inventory.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.inventory.internal.domain.InventoryItem;
import vn.ecommerce.catalog.inventory.internal.dto.InventoryRow;
import vn.ecommerce.catalog.inventory.internal.dto.InventoryUpdateCommand;
import vn.ecommerce.catalog.inventory.internal.repo.InventoryItemRepository;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class StockAdminServiceImpl implements StockAdminService {

    /** Giới hạn số SKU khớp từ khóa đưa vào mệnh đề IN. */
    private static final int SEARCH_LIMIT = 500;
    private static final UUID UNUSED_ID = new UUID(0L, 0L);

    private final InventoryItemRepository itemRepository;
    private final VariantLookupApi variantLookupApi;
    private final InventoryProperties properties;

    @Override
    @Transactional(readOnly = true)
    public PageView<InventoryRow> page(String q, boolean lowStockOnly, int page, int size) {
        boolean anySku = q == null || q.isBlank();
        List<UUID> skuIds = anySku ? List.of(UNUSED_ID) : variantLookupApi.searchVariantIds(q, SEARCH_LIMIT);
        if (!anySku && skuIds.isEmpty()) {
            return new PageView<>(List.of(), page, size, 0, false);
        }
        Page<InventoryItem> result = itemRepository.pageAdmin(anySku, skuIds, lowStockOnly, PageRequest.of(page, size));
        Map<UUID, VariantInfo> infos = variantLookupApi.findVariants(result.getContent().stream().map(InventoryItem::getSkuId).toList())
                .stream().collect(Collectors.toMap(VariantInfo::skuId, Function.identity()));
        List<InventoryRow> rows = result.getContent().stream().map(item -> toRow(item, infos.get(item.getSkuId()))).toList();
        return new PageView<>(rows, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    public InventoryRow update(UUID skuId, InventoryUpdateCommand command) {
        VariantInfo info = variantLookupApi.findVariants(List.of(skuId)).stream().findFirst()
                .orElseThrow(() -> new NotFoundException(InventoryErrorKeys.ITEM_NOT_FOUND));
        InventoryItem item = itemRepository.findBySkuId(skuId).orElse(null);
        if (item == null) {
            item = new InventoryItem(skuId, properties.defaultLowStock());
        } else {
            if (command.version() == null) {
                throw new BadRequestException(CommonErrorKeys.VERSION_REQUIRED);
            }
            if (item.getVersion() != command.version()) {
                throw new ConflictException(CommonErrorKeys.CONCURRENT_MODIFICATION);
            }
        }
        if (command.onHand() < item.getReserved()) {
            throw new BadRequestException(InventoryErrorKeys.ON_HAND_BELOW_RESERVED, item.getReserved());
        }
        item.setOnHand(command.onHand());
        if (command.lowStockThreshold() != null) {
            item.setLowStockThreshold(command.lowStockThreshold());
        }
        InventoryItem saved = itemRepository.saveAndFlush(item);
        log.info("Cập nhật tồn kho SKU {} ({}): on_hand={}, reserved={}", info.skuCode(), skuId, saved.getOnHand(), saved.getReserved());
        return toRow(saved, info);
    }

    private static InventoryRow toRow(InventoryItem item, VariantInfo info) {
        int available = Math.max(0, item.available());
        return new InventoryRow(item.getSkuId(),
                info == null ? null : info.skuCode(),
                info == null ? null : info.productName(),
                info == null ? null : info.variantLabel(),
                item.getOnHand(), item.getReserved(), available, item.getLowStockThreshold(),
                available <= item.getLowStockThreshold(), item.getVersion());
    }
}
