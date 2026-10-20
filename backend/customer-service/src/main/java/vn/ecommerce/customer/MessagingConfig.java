package vn.ecommerce.customer;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;
import vn.ecommerce.common.events.EventRoutes;
import vn.ecommerce.common.events.customer.CustomerCreatedEvent;

/**
 * Event nghiệp vụ của customer-service đi ra RabbitMQ qua Spring Modulith (ghi event_publication trong cùng
 * transaction rồi mới gửi). Định tuyến khai báo ở đây để record event trong common-events không phụ thuộc Modulith.
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
                .select(event -> event instanceof CustomerCreatedEvent)
                .route(CustomerCreatedEvent.class, event -> RoutingTarget.forTarget(EventRoutes.EXCHANGE).andKey(EventRoutes.CUSTOMER_CREATED))
                .build();
    }
}
