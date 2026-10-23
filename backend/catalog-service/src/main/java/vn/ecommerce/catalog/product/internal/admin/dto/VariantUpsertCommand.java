package vn.ecommerce.catalog.product.internal.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

/**
 * @param price      bỏ qua khi sản phẩm GOLD_RATE (giá tính từ giá vàng hiện hành)
 * @param goldKarat  K24 | K18 | K14 | K10; bắt buộc cùng {@code goldWeight}, {@code makingFee} khi sản phẩm GOLD_RATE
 * @param goldWeight trọng lượng vàng (gam, 3 số lẻ)
 * @param makingFee  phí gia công (đ)
 */
public record VariantUpsertCommand(
        @NotBlank(message = "{validation.required}") @Size(max = 64, message = "{validation.max-length}") String skuCode,
        Map<String, String> optionValues,
        @NotNull(message = "{validation.required}") @DecimalMin(value = "0", message = "{validation.min}") BigDecimal price,
        @DecimalMin(value = "0", message = "{validation.min}") BigDecimal compareAtPrice,
        @PositiveOrZero(message = "{validation.min}") Integer weightGram,
        @Size(max = 64, message = "{validation.max-length}") String barcode,
        @Pattern(regexp = "ACTIVE|HIDDEN", message = "{validation.enum}") String status,
        Integer sortOrder,
        Long version,
        @Pattern(regexp = "K24|K18|K14|K10", message = "{validation.enum}") String goldKarat,
        @DecimalMin(value = "0.001", message = "{validation.min}") @Digits(integer = 6, fraction = 3, message = "{validation.max}") BigDecimal goldWeight,
        @DecimalMin(value = "0", message = "{validation.min}") BigDecimal makingFee) {
}
