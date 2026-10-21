package vn.ecommerce.catalog.messaging.internal;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.ecommerce.common.events.EventRoutes;

/** Exchange chung và converter JSON (Jackson 3) cho mọi listener/publisher trong service. */
@Configuration
class RabbitConfig {

    @Bean
    TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(EventRoutes.EXCHANGE).durable(true).build();
    }

    /** Header __TypeId__ trỏ tới record trong common-events; mặc định chỉ tin java.util/java.lang nên phải khai báo package tin cậy. */
    @Bean
    MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages(EventRoutes.EVENT_PACKAGES.toArray(String[]::new));
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
