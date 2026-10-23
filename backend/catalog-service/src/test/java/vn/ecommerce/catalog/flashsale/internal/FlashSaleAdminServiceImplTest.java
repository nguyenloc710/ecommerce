package vn.ecommerce.catalog.flashsale.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CreateCommand;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.FlashSaleDetail;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.ItemCommand;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSale;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleHoldRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleItemRepository;
import vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleRepository;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;

@ExtendWith(MockitoExtension.class)
class FlashSaleAdminServiceImplTest {

    private static final UUID SKU = UUID.randomUUID();
    private static final BigDecimal LIST_PRICE = new BigDecimal("200000");

    @Mock FlashSaleRepository sales;
    @Mock FlashSaleItemRepository items;
    @Mock FlashSaleHoldRepository holds;
    @Mock VariantLookupApi variants;
    @Mock AdminAuditRecorder audit;

    private FlashSaleAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FlashSaleAdminServiceImpl(sales, items, holds, variants, audit, new FlashSaleProperties(Duration.ofHours(1), 2));
    }

    private static CreateCommand command(Instant start, Instant end, ItemCommand... lines) {
        return new CreateCommand(" Sale 10.10 ", start, end, List.of(lines));
    }

    private static ItemCommand item(UUID sku, String price) {
        return new ItemCommand(sku, new BigDecimal(price), 10, 2);
    }

    private static VariantInfo sku(UUID id, boolean sellable) {
        return new VariantInfo(id, UUID.randomUUID(), "nhan", "Nhẫn", "RNG-6", Map.of("size", "6"), LIST_PRICE, null, null, sellable, LIST_PRICE);
    }

    private static Instant inHours(long h) {
        return Instant.now().plus(Duration.ofHours(h));
    }

    @Test
    void create_rejectsEndBeforeStart() {
        assertThatThrownBy(() -> service.create(command(inHours(3), inHours(2), item(SKU, "99000"))))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.INVALID_WINDOW);
        verify(sales, never()).save(any());
    }

    @Test
    void create_rejectsTooManyItems() {
        assertThatThrownBy(() -> service.create(command(inHours(1), inHours(2), item(UUID.randomUUID(), "1"), item(UUID.randomUUID(), "1"),
                item(UUID.randomUUID(), "1"))))
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.TOO_MANY_ITEMS);
    }

    @Test
    void create_rejectsDuplicateSku() {
        assertThatThrownBy(() -> service.create(command(inHours(1), inHours(2), item(SKU, "1"), item(SKU, "2"))))
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.DUPLICATE_SKU);
    }

    @Test
    void create_rejectsUnsellableSku() {
        when(variants.findVariants(anyCollection())).thenReturn(List.of(sku(SKU, false)));

        assertThatThrownBy(() -> service.create(command(inHours(1), inHours(2), item(SKU, "99000"))))
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.SKU_NOT_SELLABLE);
    }

    @Test
    void create_rejectsPriceNotLowerThanListPrice() {
        when(variants.findVariants(anyCollection())).thenReturn(List.of(sku(SKU, true)));

        assertThatThrownBy(() -> service.create(command(inHours(1), inHours(2), item(SKU, "200000"))))
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.PRICE_NOT_LOWER);
        verify(sales, never()).save(any());
    }

    @Test
    void create_rejectsOverlappingSale() {
        when(variants.findVariants(anyCollection())).thenReturn(List.of(sku(SKU, true)));
        when(items.findOverlappingSkuIds(anyCollection(), any(), any())).thenReturn(List.of(SKU));

        assertThatThrownBy(() -> service.create(command(inHours(1), inHours(2), item(SKU, "99000"))))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.OVERLAP);
        verify(sales, never()).save(any());
    }

    @Test
    void create_savesSaleAndItems_andAudits() {
        when(variants.findVariants(anyCollection())).thenReturn(List.of(sku(SKU, true)));
        when(items.findOverlappingSkuIds(anyCollection(), any(), any())).thenReturn(List.of());
        when(sales.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FlashSaleDetail detail = service.create(command(inHours(1), inHours(2), item(SKU, "99000")));

        assertThat(detail.name()).isEqualTo("Sale 10.10");
        assertThat(detail.phase()).isEqualTo("UPCOMING");
        verify(items).saveAll(any());
        verify(audit).record(eq(FlashSaleConstants.AUDIT_CREATE), eq(FlashSaleConstants.AUDIT_TARGET), anyString(), anyMap());
    }

    @Test
    void cancel_rejectsStaleVersion() {
        FlashSale sale = new FlashSale("Sale", inHours(1), inHours(2));
        when(sales.findById(sale.getId())).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> service.cancel(sale.getId(), 7L))
                .hasFieldOrPropertyWithValue("messageKey", CommonErrorKeys.CONCURRENT_MODIFICATION);
        verify(sales, never()).saveAndFlush(any());
    }

    @Test
    void cancel_rejectsEndedSale() {
        FlashSale sale = new FlashSale("Sale", inHours(-3), inHours(-1));
        when(sales.findById(sale.getId())).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> service.cancel(sale.getId(), 0L))
                .hasFieldOrPropertyWithValue("messageKey", FlashSaleErrorKeys.ALREADY_ENDED);
    }

    @Test
    void cancel_runningSale_marksCancelled_andAudits() {
        FlashSale sale = new FlashSale("Sale", inHours(-1), inHours(1));
        when(sales.findById(sale.getId())).thenReturn(Optional.of(sale));

        FlashSaleDetail detail = service.cancel(sale.getId(), 0L);

        assertThat(detail.phase()).isEqualTo("CANCELLED");
        verify(audit).record(FlashSaleConstants.AUDIT_CANCEL, FlashSaleConstants.AUDIT_TARGET, sale.getId().toString(), Map.of("from", "RUNNING"));
    }
}
