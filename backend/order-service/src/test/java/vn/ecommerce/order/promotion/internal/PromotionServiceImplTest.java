package vn.ecommerce.order.promotion.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.order.promotion.DiscountQuote;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    private static final UUID CUSTOMER = UUID.randomUUID();

    @Mock VoucherRepository vouchers;
    @Mock VoucherUsageRepository usages;
    @Mock CurrentUser currentUser;
    @Mock AdminAuditRecorder audit;

    private PromotionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromotionServiceImpl(vouchers, usages, Mappers.getMapper(PromotionMapper.class), currentUser, audit);
    }

    @Test
    void percent_isCappedByMaxDiscount() {
        Voucher v = voucher("SALE10", Voucher.VoucherType.PERCENT, "10", "30000", "0");
        when(vouchers.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(v));
        when(usages.countByVoucherIdAndCustomerId(v.getId(), CUSTOMER)).thenReturn(0L);

        DiscountQuote quote = service.quote("sale10 ", CUSTOMER, new BigDecimal("450000"));

        assertThat(quote.discount()).isEqualByComparingTo("30000");
        assertThat(quote.code()).isEqualTo("SALE10");
    }

    @Test
    void fixed_neverExceedsSubtotal() {
        Voucher v = voucher("GIAM50K", Voucher.VoucherType.FIXED, "50000", null, "0");
        when(vouchers.findByCodeIgnoreCase("GIAM50K")).thenReturn(Optional.of(v));
        when(usages.countByVoucherIdAndCustomerId(any(), any())).thenReturn(0L);

        assertThat(service.quote("GIAM50K", CUSTOMER, new BigDecimal("20000")).discount()).isEqualByComparingTo("20000");
    }

    @Test
    void belowMinSubtotal_isRejected() {
        Voucher v = voucher("BIG", Voucher.VoucherType.FIXED, "100000", null, "1000000");
        when(vouchers.findByCodeIgnoreCase("BIG")).thenReturn(Optional.of(v));
        when(usages.countByVoucherIdAndCustomerId(any(), any())).thenReturn(0L);

        assertThatThrownBy(() -> service.quote("BIG", CUSTOMER, new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class).hasMessage(PromotionErrorKeys.MIN_SUBTOTAL);
    }

    @Test
    void expired_exhausted_inactive_andAlreadyUsed_areRejected() {
        Voucher expired = voucher("OLD", Voucher.VoucherType.FIXED, "10000", null, "0");
        expired.setEndsAt(Instant.now().minus(Duration.ofDays(1)));
        when(vouchers.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.quote("OLD", CUSTOMER, BigDecimal.TEN)).hasMessage(PromotionErrorKeys.EXPIRED);

        Voucher exhausted = voucher("FULL", Voucher.VoucherType.FIXED, "10000", null, "0");
        exhausted.setUsageLimit(1);
        exhausted.incrementUsed();
        when(vouchers.findByCodeIgnoreCase("FULL")).thenReturn(Optional.of(exhausted));
        assertThatThrownBy(() -> service.quote("FULL", CUSTOMER, BigDecimal.TEN)).hasMessage(PromotionErrorKeys.EXHAUSTED);

        Voucher inactive = voucher("OFF", Voucher.VoucherType.FIXED, "10000", null, "0");
        inactive.setStatus(Voucher.VoucherStatus.INACTIVE);
        when(vouchers.findByCodeIgnoreCase("OFF")).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.quote("OFF", CUSTOMER, BigDecimal.TEN)).hasMessage(PromotionErrorKeys.INACTIVE);

        Voucher used = voucher("ONCE", Voucher.VoucherType.FIXED, "10000", null, "0");
        when(vouchers.findByCodeIgnoreCase("ONCE")).thenReturn(Optional.of(used));
        when(usages.countByVoucherIdAndCustomerId(used.getId(), CUSTOMER)).thenReturn(1L);
        assertThatThrownBy(() -> service.quote("ONCE", CUSTOMER, BigDecimal.TEN)).hasMessage(PromotionErrorKeys.ALREADY_USED);
    }

    @Test
    void unknownCode_isBadRequest() {
        when(vouchers.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.quote("NOPE", CUSTOMER, BigDecimal.TEN)).hasMessage(PromotionErrorKeys.NOT_FOUND);
    }

    @Test
    void recordUsage_incrementsCounter_andWritesUsage() {
        Voucher v = voucher("SALE10", Voucher.VoucherType.PERCENT, "10", null, "0");
        when(vouchers.findById(v.getId())).thenReturn(Optional.of(v));
        UUID orderId = UUID.randomUUID();

        service.recordUsage(v.getId(), orderId, CUSTOMER, new BigDecimal("45000"));

        assertThat(v.getUsedCount()).isEqualTo(1);
        verify(vouchers).save(v);
        verify(usages).save(any(VoucherUsage.class));
    }

    @Test
    void create_duplicateCode_isConflict_andNotSaved() {
        lenient().when(currentUser.requireId()).thenReturn("admin");
        when(vouchers.existsByCodeIgnoreCase("SALE10")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new VoucherUpsertCommand("sale10", "PERCENT", BigDecimal.TEN, null, null,
                Instant.now(), Instant.now().plus(Duration.ofDays(7)), null, null, null, null, null)))
                .isInstanceOf(ConflictException.class).hasMessage(PromotionErrorKeys.CODE_TAKEN);
        verify(vouchers, never()).save(any());
    }

    @Test
    void create_percentOver100_isBadRequest() {
        when(vouchers.existsByCodeIgnoreCase("X")).thenReturn(false);

        assertThatThrownBy(() -> service.create(new VoucherUpsertCommand("X", "PERCENT", new BigDecimal("150"), null, null,
                Instant.now(), Instant.now().plus(Duration.ofDays(7)), null, null, null, null, null)))
                .hasMessage(PromotionErrorKeys.PERCENT_RANGE);
    }

    private static Voucher voucher(String code, Voucher.VoucherType type, String value, String maxDiscount, String minSubtotal) {
        Voucher v = new Voucher(code);
        v.setType(type);
        v.setValue(new BigDecimal(value));
        v.setMaxDiscount(maxDiscount == null ? null : new BigDecimal(maxDiscount));
        v.setMinSubtotal(new BigDecimal(minSubtotal));
        v.setStartsAt(Instant.now().minus(Duration.ofHours(1)));
        v.setEndsAt(Instant.now().plus(Duration.ofDays(7)));
        return v;
    }
}
