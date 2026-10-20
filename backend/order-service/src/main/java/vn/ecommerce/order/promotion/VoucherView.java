package vn.ecommerce.order.promotion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Voucher cho admin; FE khách chỉ nhận {@link DiscountQuote}. */
public record VoucherView(UUID id, String code, String type, BigDecimal value, BigDecimal maxDiscount, BigDecimal minSubtotal,
                          Instant startsAt, Instant endsAt, Integer usageLimit, int usedCount, int perCustomerLimit,
                          String status, String description, long version) {
}
