package vn.ecommerce.catalog.flashsale.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleQuota.AcquireResult;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleQuota.Claim;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleQuota.Taken;
import vn.ecommerce.catalog.flashsale.internal.domain.ActiveItem;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleHold;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleHoldRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleItemRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.HoldStatus;
import vn.ecommerce.catalog.flashsale.internal.domain.ItemQuantity;
import vn.ecommerce.catalog.inventory.ReservationGuard;
import vn.ecommerce.common.web.error.ConflictException;

/**
 * Giữ suất flash sale cùng lúc giữ hàng. Valkey trừ suất nguyên tử để chịu tải lúc mở bán; bản ghi
 * {@link FlashSaleHold} trong DB là nguồn sự thật để trả suất idempotent và khởi tạo lại bộ đếm.
 * <p>
 * Chỉ dòng nào được báo giá bằng giá flash sale (đơn giá không cao hơn giá sale) mới cần suất; khách mua giá thường
 * sau khi hết suất không bị chặn. Không dùng {@code @Transactional}: inventory gọi guard ngoài transaction của nó.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FlashSaleReservationGuard implements ReservationGuard {

    private final FlashSaleItemRepository items;
    private final FlashSaleHoldRepository holds;
    private final FlashSaleQuota quota;
    private final FlashSaleProperties properties;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void acquire(UUID orderId, String customerId, List<Line> lines) {
        Map<UUID, Line> bySku = lines.stream().collect(Collectors.toMap(Line::skuId, Function.identity(), (a, b) -> a));
        Instant now = Instant.now();
        List<ActiveItem> claimed = items.findActiveBySkuIdIn(bySku.keySet(), now).stream()
                .filter(item -> claimsSalePrice(bySku.get(item.skuId()), item)).toList();
        if (claimed.isEmpty() || !holds.findByOrderId(orderId).isEmpty()) {
            return;
        }
        List<Claim> claims = toClaims(customerId, claimed, bySku, now);
        rejectIfRefused(quota.acquire(customerId, claims), claimed);
        List<Taken> taken = claims.stream().map(c -> new Taken(c.itemId(), c.quantity())).toList();
        try {
            transactionTemplate.executeWithoutResult(status -> holds.saveAll(
                    claims.stream().map(c -> new FlashSaleHold(orderId, c.itemId(), customerId, c.quantity())).toList()));
            log.info("Đơn {} giữ {} suất flash sale", orderId, claims.size());
        } catch (DataIntegrityViolationException e) {
            // Hai lần gọi đồng thời cho cùng đơn: lần kia đã ghi hold, trả lại phần lần này vừa trừ
            log.info("Đơn {} đã giữ suất flash sale ở lần gọi khác, trả phần trừ thừa", orderId);
            safeRelease(orderId, customerId, taken);
        } catch (RuntimeException e) {
            safeRelease(orderId, customerId, taken);
            throw e;
        }
    }

    @Override
    public void release(UUID orderId) {
        List<FlashSaleHold> released = transactionTemplate.execute(status -> {
            List<FlashSaleHold> changed = new ArrayList<>();
            for (FlashSaleHold hold : holds.findByOrderId(orderId)) {
                HoldStatus before = hold.release();
                if (before == HoldStatus.COMMITTED && items.decrementSold(hold.getFlashSaleItemId(), hold.getQuantity(), Instant.now()) == 0) {
                    log.warn("Đơn {} trả suất đã bán nhưng sold của item {} không đủ để trừ", orderId, hold.getFlashSaleItemId());
                }
                if (before != null) {
                    changed.add(hold);
                }
            }
            return holds.saveAll(changed);
        });
        if (released == null || released.isEmpty()) {
            return;
        }
        quota.release(released.getFirst().getCustomerId(), released.stream().map(h -> new Taken(h.getFlashSaleItemId(), h.getQuantity())).toList());
        log.info("Đơn {} trả {} suất flash sale", orderId, released.size());
    }

    @Override
    public void commit(UUID orderId) {
        transactionTemplate.executeWithoutResult(status -> {
            List<FlashSaleHold> committed = holds.findByOrderId(orderId).stream().filter(FlashSaleHold::commit).toList();
            holds.saveAll(committed);
            for (FlashSaleHold hold : committed) {
                if (items.incrementSold(hold.getFlashSaleItemId(), hold.getQuantity(), Instant.now()) == 0) {
                    // Chỉ xảy ra khi Valkey mất dữ liệu giữa chừng và khởi tạo lệch; đơn đã xác nhận nên không chặn
                    log.warn("Đơn {} chốt suất vượt quota của item {}", orderId, hold.getFlashSaleItemId());
                }
            }
        });
    }

    private static boolean claimsSalePrice(Line line, ActiveItem item) {
        return line.unitPrice() == null || line.unitPrice().compareTo(item.salePrice()) <= 0;
    }

    private List<Claim> toClaims(String customerId, List<ActiveItem> claimed, Map<UUID, Line> bySku, Instant now) {
        Map<UUID, Long> used = customerId == null ? Map.of()
                : holds.sumUsedByCustomer(claimed.stream().map(ActiveItem::itemId).toList(), customerId).stream()
                        .collect(Collectors.toMap(ItemQuantity::itemId, ItemQuantity::quantity));
        return claimed.stream().map(item -> new Claim(item.itemId(), bySku.get(item.skuId()).quantity(),
                customerId == null ? 0 : item.perCustomerLimit(), item.remaining(), used.getOrDefault(item.itemId(), 0L),
                Duration.between(now, item.endsAt()).plus(properties.keyGrace()))).toList();
    }

    private static void rejectIfRefused(AcquireResult result, List<ActiveItem> claimed) {
        if (result.ok()) {
            return;
        }
        ActiveItem item = claimed.stream().filter(i -> i.itemId().equals(result.rejectedItemId())).findFirst().orElseThrow();
        if (result.limitExceeded()) {
            throw new ConflictException(FlashSaleErrorKeys.LIMIT_EXCEEDED, item.perCustomerLimit());
        }
        throw new ConflictException(FlashSaleErrorKeys.SOLD_OUT, item.skuId().toString());
    }

    private void safeRelease(UUID orderId, String customerId, List<Taken> taken) {
        try {
            quota.release(customerId, taken);
        } catch (RuntimeException e) {
            log.warn("Không trả được suất Valkey cho đơn {}: {}", orderId, e.getMessage());
        }
    }
}
