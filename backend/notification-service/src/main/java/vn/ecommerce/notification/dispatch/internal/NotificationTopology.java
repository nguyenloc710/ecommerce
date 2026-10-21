package vn.ecommerce.notification.dispatch.internal;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.ecommerce.common.events.EventRoutes;

/**
 * Queue của notification nghe mọi event cần gửi thông báo. Message lỗi quá số lần retry đi vào DLQ {@value #DLQ}
 * (routing key cố định = tên queue để binding DLQ khớp).
 */
@Configuration
class NotificationTopology {

    static final String QUEUE = "notification.events";
    static final String DLX = EventRoutes.EXCHANGE + ".dlx";
    static final String DLQ = QUEUE + ".dlq";

    @Bean
    Declarables notificationDeclarables(TopicExchange eventsExchange) {
        Queue queue = QueueBuilder.durable(QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(QUEUE).build();
        DirectExchange dlx = new DirectExchange(DLX, true, false);
        Queue dlq = QueueBuilder.durable(DLQ).build();
        return new Declarables(
                queue, dlx, dlq,
                BindingBuilder.bind(dlq).to(dlx).with(QUEUE),
                BindingBuilder.bind(queue).to(eventsExchange).with(EventRoutes.CUSTOMER_CREATED),
                BindingBuilder.bind(queue).to(eventsExchange).with(EventRoutes.ORDER_CONFIRMED),
                BindingBuilder.bind(queue).to(eventsExchange).with(EventRoutes.ORDER_CANCELLED),
                BindingBuilder.bind(queue).to(eventsExchange).with(EventRoutes.ORDER_SHIPPED),
                BindingBuilder.bind(queue).to(eventsExchange).with(EventRoutes.ORDER_DELIVERED));
    }
}
