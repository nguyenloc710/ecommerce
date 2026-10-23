package vn.ecommerce.catalog.product.internal.gold;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Hợp đồng API giá vàng. */
public final class GoldRateDtos {

    private GoldRateDtos() {
    }

    /** @param pricePerGram giá bán ra mỗi gam (đ) */
    public record SetRateCommand(
            @NotNull(message = "{validation.required}") @Pattern(regexp = "K24|K18|K14|K10", message = "{validation.enum}") String karat,
            @NotNull(message = "{validation.required}") @Positive(message = "{validation.min}")
            @DecimalMax(value = "100000000", message = "{validation.max}") BigDecimal pricePerGram,
            @Size(max = 255, message = "{validation.max-length}") String note) {
    }

    public record GoldRateView(UUID id, String karat, BigDecimal pricePerGram, Instant effectiveAt, String note, String createdBy) {
    }

    /** @param repricedVariants số variant GOLD_RATE vừa được tính lại giá */
    public record SetRateResult(GoldRateView rate, int repricedVariants) {
    }
}
