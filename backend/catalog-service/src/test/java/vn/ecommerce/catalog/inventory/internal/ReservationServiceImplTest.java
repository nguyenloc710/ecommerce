package vn.ecommerce.catalog.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import vn.ecommerce.catalog.inventory.internal.domain.InventoryItem;
import vn.ecommerce.catalog.inventory.internal.domain.Reservation;
import vn.ecommerce.catalog.inventory.internal.dto.ReservationResult;
import vn.ecommerce.catalog.inventory.internal.dto.ReserveRequest;
import vn.ecommerce.catalog.inventory.internal.repo.InventoryItemRepository;
import vn.ecommerce.catalog.inventory.internal.repo.ReservationRepository;
import vn.ecommerce.catalog.messaging.ProcessedEvents;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.common.web.error.ConflictException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final UUID ORDER = UUID.randomUUID();
    private static final UUID SKU = UUID.randomUUID();

    @Mock InventoryItemRepository itemRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock VariantLookupApi variantLookupApi;
    @Mock ProcessedEvents processedEvents;

    private ReservationServiceImpl service;

    /** TransactionTemplate giả: chạy callback trực tiếp, không cần PlatformTransactionManager. */
    private static TransactionTemplate directTemplate() {
        return new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
    }

    @BeforeEach
    void setUp() {
        service = new ReservationServiceImpl(itemRepository, reservationRepository, variantLookupApi, processedEvents,
                directTemplate(), new InventoryProperties(Duration.ofMinutes(15), 3, 3, 200));
    }

    private VariantInfo sellable(UUID skuId) {
        return new VariantInfo(skuId, UUID.randomUUID(), "p", "P", "SKU", Map.of("size", "6"), BigDecimal.TEN, null, null, true);
    }

    @Test
    void reserve_rejectsWhenInsufficient_andWritesNothing() {
        when(reservationRepository.findByOrderId(ORDER)).thenReturn(List.of());
        when(variantLookupApi.findVariants(anyCollection())).thenReturn(List.of(sellable(SKU)));
        InventoryItem item = new InventoryItem(SKU, 3);
        item.setOnHand(1);
        when(itemRepository.findBySkuIdIn(anyCollection())).thenReturn(List.of(item));

        assertThatThrownBy(() -> service.reserve(ORDER, List.of(new ReserveRequest.Line(SKU, 2))))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", InventoryErrorKeys.INSUFFICIENT);
        verify(itemRepository, never()).saveAll(any());
        verify(reservationRepository, never()).saveAll(any());
        assertThat(item.getReserved()).isZero();
    }

    @Test
    void reserve_rejectsUnsellableSku() {
        when(reservationRepository.findByOrderId(ORDER)).thenReturn(List.of());
        when(variantLookupApi.findVariants(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> service.reserve(ORDER, List.of(new ReserveRequest.Line(SKU, 1))))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", InventoryErrorKeys.SKU_NOT_SELLABLE);
    }

    @Test
    void reserve_mergesDuplicateLines_andReservesAllOrNothing() {
        when(reservationRepository.findByOrderId(ORDER)).thenReturn(List.of());
        when(variantLookupApi.findVariants(anyCollection())).thenReturn(List.of(sellable(SKU)));
        InventoryItem item = new InventoryItem(SKU, 3);
        item.setOnHand(5);
        when(itemRepository.findBySkuIdIn(anyCollection())).thenReturn(List.of(item));

        ReservationResult result = service.reserve(ORDER, List.of(new ReserveRequest.Line(SKU, 2), new ReserveRequest.Line(SKU, 1)));

        assertThat(item.getReserved()).isEqualTo(3);
        assertThat(item.available()).isEqualTo(2);
        assertThat(result.status()).isEqualTo("RESERVED");
        assertThat(result.lines()).hasSize(1);
        assertThat(result.lines().get(0).quantity()).isEqualTo(3);
        assertThat(result.expiresAt()).isNotNull();
    }

    @Test
    void reserve_isIdempotent_whenOrderAlreadyReserved() {
        Reservation existing = new Reservation(ORDER, SKU, 2, java.time.Instant.now());
        when(reservationRepository.findByOrderId(ORDER)).thenReturn(List.of(existing));

        ReservationResult result = service.reserve(ORDER, List.of(new ReserveRequest.Line(SKU, 5)));

        assertThat(result.lines().get(0).quantity()).isEqualTo(2);
        verify(variantLookupApi, never()).findVariants(anyCollection());
        verify(itemRepository, never()).saveAll(any());
    }

    @Test
    void commit_deductsOnHand_andIsIdempotent() {
        Reservation reservation = new Reservation(ORDER, SKU, 2, java.time.Instant.now());
        InventoryItem item = new InventoryItem(SKU, 3);
        item.setOnHand(5);
        item.reserve(2);
        when(reservationRepository.findByOrderId(ORDER)).thenReturn(List.of(reservation));
        when(itemRepository.findBySkuIdIn(anyCollection())).thenReturn(List.of(item));

        service.commit(ORDER);
        service.commit(ORDER);

        assertThat(item.getOnHand()).isEqualTo(3);
        assertThat(item.getReserved()).isZero();
        assertThat(reservation.getStatus().name()).isEqualTo("COMMITTED");
    }

    @Test
    void commitFromEvent_skipsDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        when(processedEvents.markProcessed(eventId, "inventory")).thenReturn(true, false);
        Reservation reservation = new Reservation(ORDER, SKU, 1, java.time.Instant.now());
        InventoryItem item = new InventoryItem(SKU, 3);
        item.setOnHand(1);
        item.reserve(1);
        when(reservationRepository.findByOrderId(ORDER)).thenReturn(List.of(reservation));
        when(itemRepository.findBySkuIdIn(anyCollection())).thenReturn(List.of(item));

        assertThat(service.commitFromEvent(eventId, ORDER)).isTrue();
        assertThat(service.commitFromEvent(eventId, ORDER)).isFalse();
        verify(reservationRepository, times(1)).findByOrderId(ORDER);
    }

    @Test
    void withRetry_retriesOptimisticLock_thenGivesUpAsBusy() {
        AtomicInteger calls = new AtomicInteger();
        when(reservationRepository.findByOrderId(ORDER)).thenAnswer(inv -> {
            calls.incrementAndGet();
            throw new OptimisticLockingFailureException("đụng version");
        });

        assertThatThrownBy(() -> service.release(ORDER))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", InventoryErrorKeys.BUSY);
        assertThat(calls.get()).isEqualTo(3);
    }
}
