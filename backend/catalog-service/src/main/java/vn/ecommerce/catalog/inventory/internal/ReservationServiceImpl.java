package vn.ecommerce.catalog.inventory.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.ecommerce.catalog.inventory.internal.domain.InventoryItem;
import vn.ecommerce.catalog.inventory.internal.domain.Reservation;
import vn.ecommerce.catalog.inventory.internal.domain.ReservationStatus;
import vn.ecommerce.catalog.inventory.internal.dto.ReservationResult;
import vn.ecommerce.catalog.inventory.internal.dto.ReserveRequest;
import vn.ecommerce.catalog.inventory.internal.repo.InventoryItemRepository;
import vn.ecommerce.catalog.inventory.internal.repo.ReservationRepository;
import vn.ecommerce.catalog.messaging.ProcessedEvents;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;

/**
 * Không dùng {@code @Transactional} ở class: mỗi lần thử là một transaction riêng qua {@link TransactionTemplate}
 * để có thể thử lại khi optimistic lock thất bại (transaction cũ đã rollback).
 */
@Slf4j
@Service
@RequiredArgsConstructor
class ReservationServiceImpl implements ReservationService {

    private static final String CONSUMER = "inventory";

    private final InventoryItemRepository itemRepository;
    private final ReservationRepository reservationRepository;
    private final VariantLookupApi variantLookupApi;
    private final ProcessedEvents processedEvents;
    private final TransactionTemplate transactionTemplate;
    private final InventoryProperties properties;

    @Override
    public ReservationResult reserve(UUID orderId, List<ReserveRequest.Line> lines) {
        Map<UUID, Integer> wanted = mergeLines(lines);
        return withRetry(() -> {
            List<Reservation> existing = reservationRepository.findByOrderId(orderId);
            if (!existing.isEmpty()) {
                log.info("Giữ hàng đơn {} đã có, trả kết quả cũ (idempotent)", orderId);
                return toResult(orderId, existing);
            }
            assertSellable(wanted.keySet());
            Map<UUID, InventoryItem> items = itemRepository.findBySkuIdIn(wanted.keySet()).stream()
                    .collect(Collectors.toMap(InventoryItem::getSkuId, Function.identity()));
            List<UUID> insufficient = wanted.entrySet().stream()
                    .filter(e -> items.get(e.getKey()) == null || !items.get(e.getKey()).canReserve(e.getValue()))
                    .map(Map.Entry::getKey).toList();
            if (!insufficient.isEmpty()) {
                throw new ConflictException(InventoryErrorKeys.INSUFFICIENT, join(insufficient));
            }
            Instant expiresAt = Instant.now().plus(properties.reservationTtl());
            List<Reservation> created = new ArrayList<>();
            wanted.forEach((skuId, qty) -> {
                items.get(skuId).reserve(qty);
                created.add(new Reservation(orderId, skuId, qty, expiresAt));
            });
            itemRepository.saveAll(items.values());
            reservationRepository.saveAll(created);
            reservationRepository.flush();
            log.info("Giữ hàng đơn {}: {} SKU, hết hạn {}", orderId, created.size(), expiresAt);
            return toResult(orderId, created);
        });
    }

    @Override
    public ReservationResult commit(UUID orderId) {
        return withRetry(() -> commitInTx(orderId));
    }

    @Override
    public ReservationResult release(UUID orderId) {
        return withRetry(() -> releaseInTx(orderId));
    }

    @Override
    public ReservationResult status(UUID orderId) {
        List<Reservation> reservations = reservationRepository.findByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw new NotFoundException(InventoryErrorKeys.RESERVATION_NOT_FOUND);
        }
        return toResult(orderId, reservations);
    }

    @Override
    public boolean commitFromEvent(UUID eventId, UUID orderId) {
        return withRetry(() -> {
            if (!processedEvents.markProcessed(eventId, CONSUMER)) {
                return false;
            }
            commitInTx(orderId);
            return true;
        });
    }

    @Override
    public boolean releaseFromEvent(UUID eventId, UUID orderId) {
        return withRetry(() -> {
            if (!processedEvents.markProcessed(eventId, CONSUMER)) {
                return false;
            }
            releaseInTx(orderId);
            return true;
        });
    }

    @Override
    public int releaseExpired() {
        List<UUID> orderIds = reservationRepository.findExpiredOrderIds(ReservationStatus.RESERVED, Instant.now(),
                PageRequest.of(0, properties.expiryBatchSize()));
        int released = 0;
        for (UUID orderId : orderIds) {
            try {
                withRetry(() -> releaseInTx(orderId));
                released++;
            } catch (RuntimeException ex) {
                // Một đơn lỗi không chặn các đơn khác; lần chạy sau sẽ thử lại
                log.warn("Không nhả được hàng hết hạn của đơn {}: {}", orderId, ex.getMessage());
            }
        }
        if (released > 0) {
            log.info("Đã nhả hàng {} đơn quá hạn giữ", released);
        }
        return released;
    }

    // ---- trong transaction ----

    private ReservationResult commitInTx(UUID orderId) {
        List<Reservation> reservations = loadReservations(orderId);
        Map<UUID, InventoryItem> items = itemsFor(reservations);
        for (Reservation r : reservations) {
            InventoryItem item = items.get(r.getSkuId());
            switch (r.getStatus()) {
                case RESERVED -> {
                    item.commit(r.getQuantity());
                    r.markCommitted();
                }
                case RELEASED -> {
                    // Thanh toán về sau khi hết hạn giữ: bán thẳng nếu còn hàng, không thì báo thiếu
                    if (!item.canReserve(r.getQuantity())) {
                        throw new ConflictException(InventoryErrorKeys.INSUFFICIENT, r.getSkuId().toString());
                    }
                    item.deduct(r.getQuantity());
                    r.markCommitted();
                    log.warn("Đơn {} chốt sau khi reservation SKU {} đã hết hạn, bán thẳng từ tồn", orderId, r.getSkuId());
                }
                case COMMITTED -> { /* idempotent */ }
            }
        }
        itemRepository.saveAll(items.values());
        reservationRepository.saveAll(reservations);
        reservationRepository.flush();
        return toResult(orderId, reservations);
    }

    private ReservationResult releaseInTx(UUID orderId) {
        List<Reservation> reservations = loadReservations(orderId);
        Map<UUID, InventoryItem> items = itemsFor(reservations);
        for (Reservation r : reservations) {
            if (r.isActive()) {
                items.get(r.getSkuId()).release(r.getQuantity());
                r.markReleased();
            }
        }
        itemRepository.saveAll(items.values());
        reservationRepository.saveAll(reservations);
        reservationRepository.flush();
        return toResult(orderId, reservations);
    }

    private List<Reservation> loadReservations(UUID orderId) {
        List<Reservation> reservations = reservationRepository.findByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw new NotFoundException(InventoryErrorKeys.RESERVATION_NOT_FOUND);
        }
        return reservations;
    }

    private Map<UUID, InventoryItem> itemsFor(List<Reservation> reservations) {
        List<UUID> skuIds = reservations.stream().map(Reservation::getSkuId).toList();
        return itemRepository.findBySkuIdIn(skuIds).stream().collect(Collectors.toMap(InventoryItem::getSkuId, Function.identity()));
    }

    private void assertSellable(Iterable<UUID> skuIds) {
        List<UUID> ids = new ArrayList<>();
        skuIds.forEach(ids::add);
        Map<UUID, VariantInfo> found = variantLookupApi.findVariants(ids).stream()
                .collect(Collectors.toMap(VariantInfo::skuId, Function.identity()));
        List<UUID> bad = ids.stream().filter(id -> found.get(id) == null || !found.get(id).sellable()).toList();
        if (!bad.isEmpty()) {
            throw new ConflictException(InventoryErrorKeys.SKU_NOT_SELLABLE, join(bad));
        }
    }

    // ---- helpers ----

    private <T> T withRetry(Supplier<T> action) {
        int attempts = Math.max(1, properties.optimisticRetries());
        for (int attempt = 1; ; attempt++) {
            try {
                return transactionTemplate.execute(status -> action.get());
            } catch (OptimisticLockingFailureException ex) {
                if (attempt >= attempts) {
                    log.warn("Tồn kho đang bận sau {} lần thử", attempts);
                    throw new ConflictException(InventoryErrorKeys.BUSY);
                }
                log.debug("Optimistic lock lần {}, thử lại", attempt);
            }
        }
    }

    private static Map<UUID, Integer> mergeLines(List<ReserveRequest.Line> lines) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        lines.forEach(line -> merged.merge(line.skuId(), line.quantity(), Integer::sum));
        return merged;
    }

    private static ReservationResult toResult(UUID orderId, List<Reservation> reservations) {
        boolean anyActive = reservations.stream().anyMatch(Reservation::isActive);
        boolean allCommitted = reservations.stream().allMatch(r -> r.getStatus() == ReservationStatus.COMMITTED);
        String status = anyActive ? ReservationStatus.RESERVED.name()
                : allCommitted ? ReservationStatus.COMMITTED.name() : ReservationStatus.RELEASED.name();
        Instant expiresAt = reservations.stream().map(Reservation::getExpiresAt).min(Instant::compareTo).orElse(null);
        return new ReservationResult(orderId, status, expiresAt, reservations.stream()
                .map(r -> new ReservationResult.Line(r.getSkuId(), r.getQuantity(), r.getStatus().name())).toList());
    }

    private static String join(List<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(", "));
    }
}
