package vn.ecommerce.catalog.product.internal;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.ecommerce.catalog.product.PriceAdjuster;
import vn.ecommerce.catalog.product.PriceAdjuster.AdjustedPrice;

/**
 * Gom các {@link PriceAdjuster} (0..n bean) và áp giá cho SKU. Nhiều nguồn cùng điều chỉnh một SKU thì lấy giá thấp nhất.
 */
@Component
@RequiredArgsConstructor
class PriceAdjustments {

    private final List<PriceAdjuster> adjusters;

    Map<UUID, AdjustedPrice> current(Collection<UUID> skuIds) {
        if (skuIds.isEmpty() || adjusters.isEmpty()) {
            return Map.of();
        }
        Map<UUID, AdjustedPrice> best = new HashMap<>();
        for (PriceAdjuster adjuster : adjusters) {
            adjuster.currentPrices(skuIds).forEach((sku, price) -> best.merge(sku, price,
                    (a, b) -> a.price().compareTo(b.price()) <= 0 ? a : b));
        }
        return best;
    }

    /** Giá gạch khi đang điều chỉnh: giá gạch gốc nếu cao hơn, không thì giá bán gốc. */
    static BigDecimal compareAtWhenAdjusted(BigDecimal originalPrice, BigDecimal originalCompareAt) {
        return originalCompareAt != null && originalCompareAt.compareTo(originalPrice) > 0 ? originalCompareAt : originalPrice;
    }
}
