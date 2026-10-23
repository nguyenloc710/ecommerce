package vn.ecommerce.catalog.product.internal.gold;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.product.internal.domain.GoldKarat;
import vn.ecommerce.catalog.product.internal.domain.GoldRate;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.common.web.error.BadRequestException;

/** Cách tính giá theo vàng (B-09): trọng lượng × giá vàng hiện hành của loại vàng + phí gia công, làm tròn. */
@Component
@RequiredArgsConstructor
public class GoldPricing {

    private final GoldRateRepository rates;
    private final GoldProperties properties;

    public BigDecimal price(BigDecimal weight, BigDecimal pricePerGram, BigDecimal makingFee) {
        BigDecimal raw = weight.multiply(pricePerGram).add(makingFee);
        BigDecimal unit = properties.priceRounding();
        return raw.divide(unit, 0, RoundingMode.HALF_UP).multiply(unit).setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * Gán giá variant theo giá vàng hiện hành; thiếu trường giá vàng hoặc loại vàng chưa có giá thì báo lỗi
     * (không để variant GOLD_RATE mang giá nhập tay).
     */
    @Transactional(readOnly = true)
    public void applyCurrentRate(ProductVariant variant) {
        if (!variant.hasGoldPricing()) {
            throw new BadRequestException(GoldErrorKeys.FIELDS_REQUIRED);
        }
        GoldRate rate = rates.findFirstByKaratOrderByEffectiveAtDesc(variant.getGoldKarat())
                .orElseThrow(() -> new BadRequestException(GoldErrorKeys.RATE_MISSING, variant.getGoldKarat().name()));
        variant.setPrice(price(variant.getGoldWeight(), rate.getPricePerGram(), variant.getMakingFee()));
    }

    @Transactional(readOnly = true)
    public Map<GoldKarat, GoldRate> currentRates() {
        return rates.findCurrent().stream().collect(Collectors.toMap(GoldRate::getKarat, Function.identity(), (a, b) -> a));
    }
}
