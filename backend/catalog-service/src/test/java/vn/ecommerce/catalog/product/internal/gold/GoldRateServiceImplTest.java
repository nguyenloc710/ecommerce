package vn.ecommerce.catalog.product.internal.gold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.GoldKarat;
import vn.ecommerce.catalog.product.internal.domain.Product;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.SetRateCommand;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.SetRateResult;
import vn.ecommerce.catalog.product.internal.repo.ProductVariantRepository;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.web.error.BadRequestException;

@ExtendWith(MockitoExtension.class)
class GoldRateServiceImplTest {

    @Mock GoldRateRepository rates;
    @Mock ProductVariantRepository variants;
    @Mock AdminAuditRecorder audit;

    private GoldPricing pricing;
    private GoldRateServiceImpl service;

    @BeforeEach
    void setUp() {
        pricing = new GoldPricing(rates, new GoldProperties(BigDecimal.valueOf(1000)));
        service = new GoldRateServiceImpl(rates, variants, pricing, audit);
    }

    private static ProductVariant goldVariant(String weight, String fee) {
        ProductVariant v = new ProductVariant(new Product(new Category("Nhẫn", "nhan", null), "Nhẫn vàng", "nhan-vang"), "GLD", BigDecimal.ONE);
        v.setGoldKarat(GoldKarat.K18);
        v.setGoldWeight(new BigDecimal(weight));
        v.setMakingFee(new BigDecimal(fee));
        return v;
    }

    @Test
    void price_roundsHalfUpToConfiguredUnit() {
        assertThat(pricing.price(new BigDecimal("1.000"), new BigDecimal("1000500"), BigDecimal.ZERO)).isEqualByComparingTo("1001000");
        assertThat(pricing.price(new BigDecimal("1.000"), new BigDecimal("1000499"), BigDecimal.ZERO)).isEqualByComparingTo("1000000");
    }

    @Test
    void setRate_savesRate_repricesGoldVariants_andAudits() {
        when(rates.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ProductVariant ring = goldVariant("2.000", "300000");
        ProductVariant chain = goldVariant("5.125", "800000");
        when(variants.findGoldPricedByKarat(GoldKarat.K18)).thenReturn(List.of(ring, chain));

        SetRateResult result = service.setRate(new SetRateCommand("k18", new BigDecimal("1300000"), "  Giá SJC sáng  "));

        assertThat(result.repricedVariants()).isEqualTo(2);
        assertThat(result.rate().karat()).isEqualTo("K18");
        assertThat(result.rate().note()).isEqualTo("Giá SJC sáng");
        assertThat(ring.getPrice()).isEqualByComparingTo("2900000");
        // 5,125 × 1.300.000 + 800.000 = 7.462.500 → 7.463.000
        assertThat(chain.getPrice()).isEqualByComparingTo("7463000");
        verify(variants).saveAll(List.of(ring, chain));
        verify(audit).record(eq("GOLD_RATE_SET"), eq("GOLD_RATE"), eq("K18"), eq(Map.of("pricePerGram", "1300000", "repricedVariants", 2)));
    }

    @Test
    void setRate_rejectsUnknownKarat_withoutSaving() {
        assertThatThrownBy(() -> service.setRate(new SetRateCommand("K99", BigDecimal.TEN, null)))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("messageKey", "validation.enum");
        verify(rates, never()).save(any());
    }
}
