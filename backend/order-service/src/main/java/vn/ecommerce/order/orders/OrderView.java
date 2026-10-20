package vn.ecommerce.order.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.ecommerce.order.shipping.ShipmentView;

/** Đơn hàng đầy đủ cho khách và admin. {@code shipment} null khi chưa tạo vận đơn. */
public record OrderView(UUID id, String code, String status, BigDecimal subtotal, BigDecimal discount, BigDecimal shippingFee,
                        BigDecimal total, String voucherCode, String paymentMethod, String shippingCarrier,
                        ShippingAddress shippingAddress, String contactEmail, String note, Instant placedAt, Instant paidAt,
                        Instant cancelledAt, String cancelReason, List<Item> items, List<History> history,
                        ShipmentView shipment, long version) {

    public record Item(UUID skuId, UUID productId, String productSlug, String productName, String variantLabel, String skuCode,
                       String imageUrl, BigDecimal unitPrice, int quantity, String engraving, BigDecimal lineTotal) {
    }

    public record History(String from, String to, String reason, String actorId, Instant at) {
    }
}
