package vn.ecommerce.catalog.review.internal.messaging;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import vn.ecommerce.catalog.review.internal.ReviewService;
import vn.ecommerce.common.events.order.OrderDeliveredEvent;

/** Đơn giao thành công → mở quyền đánh giá các SKU trong đơn (B-10). Exception để Spring AMQP retry rồi đưa DLQ. */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = ReviewEventsTopology.QUEUE)
class DeliveredEventsListener {

    private final ReviewService reviewService;

    @RabbitHandler
    void onDelivered(OrderDeliveredEvent event) {
        // Event phát trước E7.1 không có lines: không mở quyền được, bỏ qua
        List<OrderDeliveredEvent.Line> lines = event.lines() == null ? List.of() : event.lines();
        boolean applied = reviewService.recordDelivery(event.eventId(), event.orderId(), event.customerId(),
                lines.stream().map(OrderDeliveredEvent.Line::skuId).distinct().toList(), event.occurredAt());
        log.info("order.delivered {} đơn {}: {}", event.eventId(), event.orderCode(),
                applied ? "mở quyền đánh giá " + lines.size() + " SKU" : "bỏ qua (đã xử lý)");
    }
}
