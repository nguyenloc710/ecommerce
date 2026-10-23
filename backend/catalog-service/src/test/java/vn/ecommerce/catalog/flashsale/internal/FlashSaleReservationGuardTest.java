package vn.ecommerce.catalog.flashsale.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
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
import vn.ecommerce.catalog.inventory.ReservationGuard.Line;
import vn.ecommerce.common.web.error.ConflictException;

@ExtendWith(MockitoExtension.class)
class FlashSaleReservationGuardTest {

    private static final UUID ORDER = UUID.randomUUID();
    private static final UUID SKU = UUID.randomUUID();
    private static final UUID ITEM = UUID.randomUUID();
    private static final String CUSTOMER = "kh-1";
    private static final BigDecimal SALE_PRICE = new BigDecimal("99000");

    @Mock FlashSaleItemRepository items;
    @Mock FlashSaleHoldRepository holds;
    @Mock FlashSaleQuota quota;

    private FlashSaleReservationGuard guard;

    @BeforeEach
    void setUp() {
        TransactionTemplate direct = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
        guard = new FlashSaleReservationGuard(items, holds, quota, new FlashSaleProperties(Duration.ofHours(1), 50), direct);
    }

    /** quota 10, đã bán 3, đang giữ 2 → còn 5; giới hạn 2 mỗi khách. */
    private static ActiveItem active() {
        return new ActiveItem(ITEM, SKU, SALE_PRICE, 10, 3, 2, Instant.now().plus(Duration.ofMinutes(30)), 2);
    }

    private static List<Line> line(int quantity, BigDecimal unitPrice) {
        return List.of(new Line(SKU, quantity, unitPrice));
    }

    @Test
    void acquire_noActiveSale_doesNothing() {
        when(items.findActiveBySkuIdIn(anyCollection(), any())).thenReturn(List.of());

        guard.acquire(ORDER, CUSTOMER, line(1, SALE_PRICE));

        verifyNoInteractions(quota, holds);
    }

    @Test
    void acquire_linePricedAboveSalePrice_isNotAFlashClaim() {
        when(items.findActiveBySkuIdIn(anyCollection(), any())).thenReturn(List.of(active()));

        guard.acquire(ORDER, CUSTOMER, line(1, new BigDecimal("150000")));

        verifyNoInteractions(quota, holds);
    }

    @Test
    void acquire_existingHolds_isIdempotent() {
        when(items.findActiveBySkuIdIn(anyCollection(), any())).thenReturn(List.of(active()));
        when(holds.findByOrderId(ORDER)).thenReturn(List.of(new FlashSaleHold(ORDER, ITEM, CUSTOMER, 1)));

        guard.acquire(ORDER, CUSTOMER, line(1, SALE_PRICE));

        verifyNoInteractions(quota);
        verify(holds, never()).saveAll(any());
    }

    @Test
    void acquire_soldOut_rejectsAndWritesNothing() {
        stubClaimable();
        when(quota.acquire(eq(CUSTOMER), any())).thenReturn(new AcquireResult(ITEM, false));

        assertThatThrownBy(() -> guard.acquire(ORDER, CUSTOMER, line(1, SALE_PRICE)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.SOLD_OUT);
        verify(holds, never()).saveAll(any());
    }

    @Test
    void acquire_limitExceeded_rejectsWithLimit() {
        stubClaimable();
        when(quota.acquire(eq(CUSTOMER), any())).thenReturn(new AcquireResult(ITEM, true));

        assertThatThrownBy(() -> guard.acquire(ORDER, CUSTOMER, line(1, SALE_PRICE)))
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.LIMIT_EXCEEDED)
                .hasFieldOrPropertyWithValue("args", new Object[]{2});
        verify(holds, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void acquire_success_passesDbValuesToQuota_andSavesHold() {
        stubClaimable();
        when(quota.acquire(eq(CUSTOMER), any())).thenReturn(AcquireResult.OK);

        guard.acquire(ORDER, CUSTOMER, line(2, SALE_PRICE));

        ArgumentCaptor<List<Claim>> claims = ArgumentCaptor.forClass(List.class);
        verify(quota).acquire(eq(CUSTOMER), claims.capture());
        Claim claim = claims.getValue().getFirst();
        assertThat(claim.quantity()).isEqualTo(2);
        assertThat(claim.initialRemaining()).isEqualTo(5);
        assertThat(claim.initialUsed()).isEqualTo(1);
        assertThat(claim.perCustomerLimit()).isEqualTo(2);
        // TTL = còn 30 phút + 1 giờ ân hạn
        assertThat(claim.ttl()).isGreaterThan(Duration.ofMinutes(89));
        ArgumentCaptor<List<FlashSaleHold>> saved = ArgumentCaptor.forClass(List.class);
        verify(holds).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(h -> {
            assertThat(h.getQuantity()).isEqualTo(2);
            assertThat(h.getStatus()).isEqualTo(HoldStatus.HELD);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void acquire_anonymousCustomer_skipsPerCustomerLimit() {
        when(items.findActiveBySkuIdIn(anyCollection(), any())).thenReturn(List.of(active()));
        when(holds.findByOrderId(ORDER)).thenReturn(List.of());
        when(quota.acquire(any(), any())).thenReturn(AcquireResult.OK);

        guard.acquire(ORDER, null, line(1, null));

        verify(holds, never()).sumUsedByCustomer(anyCollection(), anyString());
        ArgumentCaptor<List<Claim>> claims = ArgumentCaptor.forClass(List.class);
        verify(quota).acquire(eq(null), claims.capture());
        assertThat(claims.getValue().getFirst().perCustomerLimit()).isZero();
    }

    @Test
    void acquire_dbWriteFails_givesQuotaBack() {
        stubClaimable();
        when(quota.acquire(eq(CUSTOMER), any())).thenReturn(AcquireResult.OK);
        when(holds.saveAll(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> guard.acquire(ORDER, CUSTOMER, line(1, SALE_PRICE))).isInstanceOf(IllegalStateException.class);
        verify(quota).release(CUSTOMER, List.of(new Taken(ITEM, 1)));
    }

    @Test
    void acquire_concurrentDuplicate_givesQuotaBackWithoutError() {
        stubClaimable();
        when(quota.acquire(eq(CUSTOMER), any())).thenReturn(AcquireResult.OK);
        when(holds.saveAll(any())).thenThrow(new DataIntegrityViolationException("uq"));

        guard.acquire(ORDER, CUSTOMER, line(1, SALE_PRICE));

        verify(quota).release(CUSTOMER, List.of(new Taken(ITEM, 1)));
    }

    @Test
    void release_heldAndCommitted_returnsQuota_andDecrementsSoldOnlyForCommitted() {
        FlashSaleHold held = new FlashSaleHold(ORDER, ITEM, CUSTOMER, 1);
        UUID otherItem = UUID.randomUUID();
        FlashSaleHold committed = new FlashSaleHold(ORDER, otherItem, CUSTOMER, 2);
        committed.commit();
        when(holds.findByOrderId(ORDER)).thenReturn(List.of(held, committed));
        when(holds.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(items.decrementSold(eq(otherItem), eq(2), any())).thenReturn(1);

        guard.release(ORDER);

        verify(items, never()).decrementSold(eq(ITEM), anyInt(), any());
        verify(quota).release(CUSTOMER, List.of(new Taken(ITEM, 1), new Taken(otherItem, 2)));
        assertThat(held.getStatus()).isEqualTo(HoldStatus.RELEASED);
        assertThat(committed.getStatus()).isEqualTo(HoldStatus.RELEASED);
    }

    @Test
    void release_alreadyReleased_isNoop() {
        FlashSaleHold hold = new FlashSaleHold(ORDER, ITEM, CUSTOMER, 1);
        hold.release();
        when(holds.findByOrderId(ORDER)).thenReturn(List.of(hold));
        when(holds.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        guard.release(ORDER);

        verifyNoInteractions(quota);
    }

    @Test
    void commit_heldBecomesSold_once() {
        FlashSaleHold hold = new FlashSaleHold(ORDER, ITEM, CUSTOMER, 2);
        when(holds.findByOrderId(ORDER)).thenReturn(List.of(hold));
        when(items.incrementSold(eq(ITEM), eq(2), any())).thenReturn(1);

        guard.commit(ORDER);
        guard.commit(ORDER);

        verify(items).incrementSold(eq(ITEM), eq(2), any());
        assertThat(hold.getStatus()).isEqualTo(HoldStatus.COMMITTED);
        verifyNoInteractions(quota);
    }

    private void stubClaimable() {
        when(items.findActiveBySkuIdIn(anyCollection(), any())).thenReturn(List.of(active()));
        when(holds.findByOrderId(ORDER)).thenReturn(List.of());
        when(holds.sumUsedByCustomer(anyCollection(), eq(CUSTOMER))).thenReturn(List.of(new ItemQuantity(ITEM, 1)));
    }
}
