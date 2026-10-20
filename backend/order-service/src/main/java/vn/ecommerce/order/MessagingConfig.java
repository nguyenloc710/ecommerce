package vn.ecommerce.order;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;
import vn.ecommerce.common.events.EventRoutes;
import vn.ecommerce.common.events.IntegrationEvent;
import vn.ecommerce.common.events.order.OrderCancelledEvent;
import vn.ecommerce.common.events.order.OrderConfirmedEvent;
import vn.ecommerce.common.events.order.OrderDeliveredEvent;
import vn.ecommerce.common.events.order.OrderShippedEvent;

/**
 * Event đơn hàng đi ra RabbitMQ qua outbox của Spring Modulith (ghi event_publication trong cùng transaction rồi mới gửi).
 * catalog-service nghe confirmed/cancelled để chốt/nhả hàng; notification-service nghe cả bốn để gửi email.
 */
@Configuration
class MessagingConfig {

    @Bean
    TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(EventRoutes.EXCHANGE).durable(true).build();
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    EventExternalizationConfiguration eventExternalization() {
        return EventExternalizationConfiguration.externalizing()
                .select(event -> event instanceof IntegrationEvent)
                .route(OrderConfirmedEvent.class, event -> RoutingTarget.forTarget(EventRoutes.EXCHANGE).andKey(EventRoutes.ORDER_CONFIRMED))
                .route(OrderCancelledEvent.class, event -> RoutingTarget.forTarget(EventRoutes.EXCHANGE).andKey(EventRoutes.ORDER_CANCELLED))
                .route(OrderShippedEvent.class, event -> RoutingTarget.forTarget(EventRoutes.EXCHANGE).andKey(EventRoutes.ORDER_SHIPPED))
                .route(OrderDeliveredEvent.class, event -> RoutingTarget.forTarget(EventRoutes.EXCHANGE).andKey(EventRoutes.ORDER_DELIVERED))
                .build();
    }
}
