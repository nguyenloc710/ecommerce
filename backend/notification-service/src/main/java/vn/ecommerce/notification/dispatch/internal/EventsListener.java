package vn.ecommerce.notification.dispatch.internal;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import vn.ecommerce.common.events.customer.CustomerCreatedEvent;
import vn.ecommerce.common.events.order.OrderCancelledEvent;
import vn.ecommerce.common.events.order.OrderConfirmedEvent;
import vn.ecommerce.common.events.order.OrderDeliveredEvent;
import vn.ecommerce.common.events.order.OrderShippedEvent;
import vn.ecommerce.notification.channel.ChannelType;

/** Mỗi event → một {@link DispatchRequest}; exception từ dispatcher ném ra để Spring AMQP retry rồi đưa vào DLQ. */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = NotificationTopology.QUEUE)
class EventsListener {

    static final String KIND_CUSTOMER_WELCOME = "CUSTOMER_WELCOME";
    static final String KIND_ORDER_CONFIRMED = "ORDER_CONFIRMED";
    static final String KIND_ORDER_CANCELLED = "ORDER_CANCELLED";
    static final String KIND_ORDER_SHIPPED = "ORDER_SHIPPED";
    static final String KIND_ORDER_DELIVERED = "ORDER_DELIVERED";

    private final NotificationDispatcher dispatcher;

    @RabbitHandler
    void on(CustomerCreatedEvent event) {
        dispatch(new DispatchRequest(event.eventId(), KIND_CUSTOMER_WELCOME, ChannelType.EMAIL, event.email(), "customer-welcome",
                Map.of("fullName", nullSafe(event.fullName()))), event.customerId());
    }

    @RabbitHandler
    void on(OrderConfirmedEvent event) {
        dispatch(new DispatchRequest(event.eventId(), KIND_ORDER_CONFIRMED, ChannelType.EMAIL, event.contactEmail(), "order-confirmed",
                Map.of("orderCode", event.orderCode(), "lines", event.lines(), "total", event.total()), event.orderCode()), event.orderCode());
    }

    @RabbitHandler
    void on(OrderCancelledEvent event) {
        dispatch(new DispatchRequest(event.eventId(), KIND_ORDER_CANCELLED, ChannelType.EMAIL, event.contactEmail(), "order-cancelled",
                Map.of("orderCode", event.orderCode(), "reason", nullSafe(event.reason())), event.orderCode()), event.orderCode());
    }

    @RabbitHandler
    void on(OrderShippedEvent event) {
        dispatch(new DispatchRequest(event.eventId(), KIND_ORDER_SHIPPED, ChannelType.EMAIL, event.contactEmail(), "order-shipped",
                Map.of("orderCode", event.orderCode(), "carrier", nullSafe(event.carrier()), "trackingCode", nullSafe(event.trackingCode())),
                event.orderCode()), event.orderCode());
    }

    @RabbitHandler
    void on(OrderDeliveredEvent event) {
        dispatch(new DispatchRequest(event.eventId(), KIND_ORDER_DELIVERED, ChannelType.EMAIL, event.contactEmail(), "order-delivered",
                Map.of("orderCode", event.orderCode()), event.orderCode()), event.orderCode());
    }

    private void dispatch(DispatchRequest request, String ref) {
        boolean sent = dispatcher.dispatch(request);
        log.info("{} {} ({}): {}", request.kind(), request.eventId(), ref, sent ? "đã xử lý" : "bỏ qua (đã gửi)");
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
