package vn.ecommerce.catalog.review.internal.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.ecommerce.common.events.EventRoutes;

/** Queue riêng của module review: order.delivered mở quyền đánh giá. Lỗi sau retry vào DLQ. */
@Configuration
class ReviewEventsTopology {

    static final String QUEUE = "catalog.review-events";
    static final String DLX = EventRoutes.EXCHANGE + ".dlx";
    static final String DLQ = QUEUE + ".dlq";

    @Bean
    Declarables reviewEventsDeclarables(TopicExchange eventsExchange) {
        Queue queue = QueueBuilder.durable(QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(QUEUE).build();
        DirectExchange dlx = new DirectExchange(DLX, true, false);
        Queue dlq = QueueBuilder.durable(DLQ).build();
        return new Declarables(
                queue, dlx, dlq,
                BindingBuilder.bind(dlq).to(dlx).with(QUEUE),
                BindingBuilder.bind(queue).to(eventsExchange).with(EventRoutes.ORDER_DELIVERED));
    }
}
