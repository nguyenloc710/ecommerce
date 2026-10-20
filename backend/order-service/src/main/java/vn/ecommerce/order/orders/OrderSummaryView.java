package vn.ecommerce.order.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Dòng trong danh sách đơn (khách và admin). */
public record OrderSummaryView(UUID id, String code, String status, BigDecimal total, String paymentMethod, String recipientName,
                               String contactEmail, int itemCount, String firstItemName, String firstItemImage, Instant placedAt) {
}
