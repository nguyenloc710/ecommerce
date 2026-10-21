package vn.ecommerce.notification;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.ecommerce.common.events.EventRoutes;

/** Exchange chung và converter JSON (Jackson 3); header {@code __TypeId__} trỏ tới record trong common-events nên phải tin package đó. */
@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(EventRoutes.EXCHANGE).durable(true).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages(EventRoutes.EVENT_PACKAGES.toArray(String[]::new));
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
