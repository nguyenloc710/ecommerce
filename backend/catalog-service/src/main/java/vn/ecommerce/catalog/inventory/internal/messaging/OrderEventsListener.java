package vn.ecommerce.catalog.inventory.internal.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import vn.ecommerce.catalog.inventory.internal.ReservationService;
import vn.ecommerce.common.events.order.OrderCancelledEvent;
import vn.ecommerce.common.events.order.OrderConfirmedEvent;

/**
 * Đơn xác nhận → chốt hàng đã giữ; đơn hủy → nhả hàng. Idempotent qua {@code processed_event};
 * exception ném ra để Spring AMQP retry rồi đưa vào DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = OrderEventsTopology.QUEUE)
class OrderEventsListener {

    private final ReservationService reservationService;

    @RabbitHandler
    void onConfirmed(OrderConfirmedEvent event) {
        boolean applied = reservationService.commitFromEvent(event.eventId(), event.orderId());
        log.info("order.confirmed {} đơn {}: {}", event.eventId(), event.orderCode(), applied ? "đã chốt kho" : "bỏ qua (đã xử lý)");
    }

    @RabbitHandler
    void onCancelled(OrderCancelledEvent event) {
        boolean applied = reservationService.releaseFromEvent(event.eventId(), event.orderId());
        log.info("order.cancelled {} đơn {}: {}", event.eventId(), event.orderCode(), applied ? "đã nhả kho" : "bỏ qua (đã xử lý)");
    }
}
