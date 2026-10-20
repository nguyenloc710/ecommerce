package vn.ecommerce.order.orders.internal;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import vn.ecommerce.order.orders.OrderSummaryView;
import vn.ecommerce.order.orders.OrderView;
import vn.ecommerce.order.orders.ShippingAddress;
import vn.ecommerce.order.shipping.ShipmentView;

/** Map entity → view; địa chỉ JSONB đi qua Jackson để không tự tay chép từng field. */
@Component
@RequiredArgsConstructor
class OrderViewMapper {

    private final ObjectMapper json;

    OrderView toView(Order order, ShipmentView shipment) {
        List<OrderView.Item> items = order.getItems().stream().map(i -> new OrderView.Item(i.getSkuId(), i.getProductId(),
                i.getProductSlug(), i.getProductName(), i.getVariantLabel(), i.getSkuCode(), i.getImageUrl(), i.getUnitPrice(),
                i.getQuantity(), i.getEngraving(), i.getLineTotal())).toList();
        List<OrderView.History> history = order.getHistory().stream().map(h -> new OrderView.History(
                h.getFromStatus() == null ? null : h.getFromStatus().name(), h.getToStatus().name(), h.getReason(), h.getActorId(),
                h.getCreatedAt())).toList();
        return new OrderView(order.getId(), order.getCode(), order.getStatus().name(), order.getSubtotal(), order.getDiscount(),
                order.getShippingFee(), order.getTotal(), order.getVoucherCode(), order.getPaymentMethod(), order.getShippingCarrier(),
                toAddress(order.getShippingAddress()), order.getContactEmail(), order.getNote(), order.getPlacedAt(), order.getPaidAt(),
                order.getCancelledAt(), order.getCancelReason(), items, history, shipment, order.getVersion());
    }

    OrderSummaryView toSummary(Order order) {
        OrderItem first = order.getItems().isEmpty() ? null : order.getItems().getFirst();
        int count = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        return new OrderSummaryView(order.getId(), order.getCode(), order.getStatus().name(), order.getTotal(), order.getPaymentMethod(),
                order.getRecipientName(), order.getContactEmail(), count, first == null ? null : first.getProductName(),
                first == null ? null : first.getImageUrl(), order.getPlacedAt());
    }

    Map<String, Object> toMap(ShippingAddress address) {
        return json.convertValue(address, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {
        });
    }

    ShippingAddress toAddress(Map<String, Object> map) {
        return json.convertValue(map, ShippingAddress.class);
    }
}
