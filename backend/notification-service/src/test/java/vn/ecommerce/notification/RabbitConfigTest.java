package vn.ecommerce.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import vn.ecommerce.common.events.customer.CustomerCreatedEvent;
import vn.ecommerce.common.events.order.OrderConfirmedEvent;

/** Message do service khác (converter mặc định, header {@code __TypeId__}) phải đọc được ở đây; trước đây rơi vào DLQ. */
class RabbitConfigTest {

    private final MessageConverter publisher = new JacksonJsonMessageConverter();
    private final MessageConverter consumer = new RabbitConfig().jsonMessageConverter();

    @Test
    void readsEventsFromCommonEventsSubpackages() {
        OrderConfirmedEvent order = new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "ORD-1", "c", "a@b.vn",
                BigDecimal.TEN, List.of(new OrderConfirmedEvent.Line(UUID.randomUUID(), "Nhẫn", "6", 1, BigDecimal.TEN)));
        CustomerCreatedEvent customer = new CustomerCreatedEvent(UUID.randomUUID(), Instant.now(), "c", "a@b.vn", "Lan");

        assertThat(roundTrip(order)).isEqualTo(order);
        assertThat(roundTrip(customer)).isEqualTo(customer);
    }

    private Object roundTrip(Object event) {
        Message message = publisher.toMessage(event, new MessageProperties());
        return consumer.fromMessage(message);
    }
}
