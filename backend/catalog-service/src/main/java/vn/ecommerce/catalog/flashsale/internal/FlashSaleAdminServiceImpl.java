package vn.ecommerce.catalog.flashsale.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CreateCommand;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.FlashSaleDetail;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.FlashSaleRow;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.ItemCommand;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.ItemRow;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSale;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleHoldRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleItem;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleItemRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSalePhase;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.ItemQuantity;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.common.web.error.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class FlashSaleAdminServiceImpl implements FlashSaleAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final FlashSaleRepository sales;
    private final FlashSaleItemRepository items;
    private final FlashSaleHoldRepository holds;
    private final VariantLookupApi variants;
    private final AdminAuditRecorder audit;
    private final FlashSaleProperties properties;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FlashSaleRow> list(int page, int size) {
        Page<FlashSale> found = sales.findAllByOrderByStartsAtDesc(PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
        Map<UUID, List<FlashSaleItem>> itemsBySale = items.findByFlashSaleIdIn(found.getContent().stream().map(FlashSale::getId).toList())
                .stream().collect(Collectors.groupingBy(FlashSaleItem::getFlashSaleId));
        Instant now = Instant.now();
        List<FlashSaleRow> rows = found.getContent().stream().map(sale -> {
            List<FlashSaleItem> saleItems = itemsBySale.getOrDefault(sale.getId(), List.of());
            return new FlashSaleRow(sale.getId(), sale.getName(), sale.getStartsAt(), sale.getEndsAt(), sale.phase(now).name(), saleItems.size(),
                    saleItems.stream().mapToLong(FlashSaleItem::getQuota).sum(), saleItems.stream().mapToLong(FlashSaleItem::getSold).sum(),
                    sale.getVersion());
        }).toList();
        return PageResponse.of(rows, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public FlashSaleDetail detail(UUID id) {
        return toDetail(require(id));
    }

    @Override
    public FlashSaleDetail create(CreateCommand command) {
        Instant now = Instant.now();
        validateWindow(command, now);
        Map<UUID, VariantInfo> skus = validateItems(command.items());
        List<UUID> overlapping = items.findOverlappingSkuIds(skus.keySet(), command.startsAt(), command.endsAt());
        if (!overlapping.isEmpty()) {
            throw new ConflictException(FlashSaleErrorKeys.OVERLAP, join(overlapping));
        }
        FlashSale sale = sales.save(new FlashSale(command.name().trim(), command.startsAt(), command.endsAt()));
        items.saveAll(command.items().stream()
                .map(i -> new FlashSaleItem(sale.getId(), i.skuId(), i.salePrice(), i.quota(), i.perCustomerLimit())).toList());
        audit.record(FlashSaleConstants.AUDIT_CREATE, FlashSaleConstants.AUDIT_TARGET, sale.getId().toString(),
                Map.of("name", sale.getName(), "startsAt", sale.getStartsAt().toString(), "endsAt", sale.getEndsAt().toString(),
                        "items", command.items().size()));
        log.info("Tạo flash sale {} ({} SKU, {} → {})", sale.getId(), command.items().size(), sale.getStartsAt(), sale.getEndsAt());
        return toDetail(sale);
    }

    @Override
    public FlashSaleDetail cancel(UUID id, Long version) {
        FlashSale sale = require(id);
        if (version == null) {
            throw new BadRequestException(CommonErrorKeys.VERSION_REQUIRED);
        }
        if (sale.getVersion() != version) {
            throw new ConflictException(CommonErrorKeys.CONCURRENT_MODIFICATION);
        }
        FlashSalePhase before = sale.phase(Instant.now());
        if (before == FlashSalePhase.ENDED) {
            throw new BadRequestException(FlashSaleErrorKeys.ALREADY_ENDED);
        }
        if (sale.cancel()) {
            sales.saveAndFlush(sale);
            audit.record(FlashSaleConstants.AUDIT_CANCEL, FlashSaleConstants.AUDIT_TARGET, id.toString(), Map.of("from", before.name()));
            log.info("Dừng flash sale {} (đang {})", id, before);
        }
        return toDetail(sale);
    }

    private void validateWindow(CreateCommand command, Instant now) {
        if (!command.endsAt().isAfter(command.startsAt()) || !command.endsAt().isAfter(now)) {
            throw new BadRequestException(FlashSaleErrorKeys.INVALID_WINDOW);
        }
    }

    /** Số lượng, trùng SKU, SKU bán được, giá sale thấp hơn giá niêm yết; trả thông tin SKU theo id. */
    private Map<UUID, VariantInfo> validateItems(List<ItemCommand> commands) {
        if (commands.size() > properties.maxItemsPerSale()) {
            throw new BadRequestException(FlashSaleErrorKeys.TOO_MANY_ITEMS, properties.maxItemsPerSale());
        }
        Set<UUID> seen = new HashSet<>();
        commands.stream().map(ItemCommand::skuId).filter(sku -> !seen.add(sku)).findFirst().ifPresent(sku -> {
            throw new BadRequestException(FlashSaleErrorKeys.DUPLICATE_SKU, sku.toString());
        });
        Map<UUID, VariantInfo> skus = lookup(seen);
        List<UUID> notSellable = seen.stream().filter(sku -> skus.get(sku) == null || !skus.get(sku).sellable()).toList();
        if (!notSellable.isEmpty()) {
            throw new BadRequestException(FlashSaleErrorKeys.SKU_NOT_SELLABLE, join(notSellable));
        }
        commands.stream().filter(c -> c.salePrice().compareTo(skus.get(c.skuId()).listPrice()) >= 0).findFirst().ifPresent(c -> {
            throw new BadRequestException(FlashSaleErrorKeys.PRICE_NOT_LOWER, skus.get(c.skuId()).skuCode());
        });
        return skus;
    }

    private FlashSale require(UUID id) {
        return sales.findById(id).orElseThrow(() -> new NotFoundException(FlashSaleErrorKeys.NOT_FOUND));
    }

    private FlashSaleDetail toDetail(FlashSale sale) {
        List<FlashSaleItem> saleItems = items.findByFlashSaleIdIn(List.of(sale.getId()));
        Map<UUID, VariantInfo> skus = lookup(saleItems.stream().map(FlashSaleItem::getSkuId).toList());
        Map<UUID, Long> held = saleItems.isEmpty() ? Map.of()
                : holds.sumHeld(saleItems.stream().map(FlashSaleItem::getId).toList()).stream()
                        .collect(Collectors.toMap(ItemQuantity::itemId, ItemQuantity::quantity));
        List<ItemRow> rows = saleItems.stream().map(i -> toRow(i, skus.get(i.getSkuId()), held.getOrDefault(i.getId(), 0L))).toList();
        return new FlashSaleDetail(sale.getId(), sale.getName(), sale.getStartsAt(), sale.getEndsAt(), sale.phase(Instant.now()).name(),
                sale.getVersion(), rows);
    }

    private static ItemRow toRow(FlashSaleItem item, VariantInfo sku, long held) {
        return new ItemRow(item.getId(), item.getSkuId(), sku == null ? null : sku.skuCode(), sku == null ? null : sku.productName(),
                sku == null ? null : sku.productSlug(), sku == null ? null : sku.variantLabel(), sku == null ? null : sku.imageUrl(),
                sku == null ? null : sku.listPrice(), item.getSalePrice(), item.getQuota(), item.getSold(), held, item.getPerCustomerLimit());
    }

    private Map<UUID, VariantInfo> lookup(Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        return variants.findVariants(List.copyOf(skuIds)).stream().collect(Collectors.toMap(VariantInfo::skuId, Function.identity()));
    }

    private static String join(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(", "));
    }
}
