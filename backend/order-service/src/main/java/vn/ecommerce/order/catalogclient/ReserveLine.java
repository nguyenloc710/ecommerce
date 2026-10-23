package vn.ecommerce.order.catalogclient;

import java.math.BigDecimal;
import java.util.UUID;

/** @param unitPrice đơn giá đã báo cho khách; catalog dùng để biết dòng này có hưởng giá flash sale (cần giữ suất) không */
public record ReserveLine(UUID skuId, int quantity, BigDecimal unitPrice) {
}
