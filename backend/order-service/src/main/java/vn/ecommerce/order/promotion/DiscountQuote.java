package vn.ecommerce.order.promotion;

import java.math.BigDecimal;
import java.util.UUID;

/** Kết quả áp mã: số tiền giảm đã trừ trần và không vượt tạm tính. */
public record DiscountQuote(UUID voucherId, String code, BigDecimal discount, String description) {
}
