package vn.ecommerce.notification.dispatch.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.ecommerce.common.events.order.OrderConfirmedEvent;
import vn.ecommerce.common.events.order.OrderShippedEvent;

/**
 * Cả ứng dụng trên PostgreSQL thật (migration + notification_log), template Thymeleaf thật; chỉ SMTP được mock.
 * Gọi listener trực tiếp như khi RabbitMQ giao message; giao lại cùng event không gửi lần hai.
 */
/*
 * @DirtiesContext: đóng context ngay sau class, khi container PostgreSQL còn sống. Không có nó, context nằm trong cache
 * tới lúc JVM thoát, Spring Modulith truy vấn event chưa xong vào DB đã bị dừng và Hikari chờ đủ 30s (E0.7).
 * Callback afterAll chạy ngược thứ tự đăng ký nên Spring đóng trước, Testcontainers dừng container sau.
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        // JavaMailSender bị mock nên health check SMTP không dựng được
        "management.health.mail.enabled=false"
})
class NotificationServiceDbTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean JavaMailSender mailSender;
    @Autowired EventsListener listener;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void stubMail() {
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));
    }

    @Test
    void orderConfirmed_rendersVietnameseEmail_once() throws Exception {
        OrderConfirmedEvent event = new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "ORD-261021-0001",
                "c-1", "lan@example.com", new BigDecimal("1780000.00"),
                List.of(new OrderConfirmedEvent.Line(UUID.randomUUID(), "Nhẫn bạc moissanite", "6 / Bạc", 2, new BigDecimal("890000.00"))));

        listener.on(event);
        listener.on(event);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(sent.capture());
        MimeMessage mail = sent.getValue();
        assertThat(mail.getSubject()).isEqualTo("Đã nhận đơn hàng ORD-261021-0001");
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("lan@example.com");
        String html = (String) mail.getContent();
        assertThat(html).contains("Nhẫn bạc moissanite", "6 / Bạc", "× 2", "1.780.000₫", "/tai-khoan/don-hang/ORD-261021-0001", "Maison");
        assertThat(jdbc.queryForObject("select count(*) from notification_log where event_id = ? and status = 'SENT'", Long.class,
                event.eventId())).isEqualTo(1L);
    }

    @Test
    void orderShipped_includesTrackingCode() throws Exception {
        listener.on(new OrderShippedEvent(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "ORD-261021-0002", "c-1",
                "lan@example.com", "GHN", "MOCK-GHN-ABCD1234"));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getSubject()).isEqualTo("Đơn hàng ORD-261021-0002 đang được giao");
        assertThat((String) sent.getValue().getContent()).contains("MOCK-GHN-ABCD1234", "GHN");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
