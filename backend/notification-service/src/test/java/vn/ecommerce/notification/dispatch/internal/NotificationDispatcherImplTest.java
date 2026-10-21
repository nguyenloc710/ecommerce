package vn.ecommerce.notification.dispatch.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.notification.NotificationProperties;
import vn.ecommerce.notification.channel.ChannelException;
import vn.ecommerce.notification.channel.ChannelType;
import vn.ecommerce.notification.channel.NotificationChannel;
import vn.ecommerce.notification.channel.OutboundMessage;
import vn.ecommerce.notification.template.RenderedMail;
import vn.ecommerce.notification.template.TemplateRenderer;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherImplTest {

    private static final UUID EVENT = UUID.randomUUID();

    @Mock NotificationLogRepository logs;
    @Mock TemplateRenderer renderer;
    @Mock NotificationChannel email;

    private NotificationDispatcherImpl dispatcher;

    @BeforeEach
    void setUp() {
        when(email.type()).thenReturn(ChannelType.EMAIL);
        dispatcher = new NotificationDispatcherImpl(logs, renderer, List.of(email),
                new NotificationProperties("Maison", "Maison <no-reply@example.vn>", "http://localhost:3000", "vi"));
    }

    @Test
    void sends_thenLogs() {
        when(logs.existsByEventIdAndChannel(EVENT, ChannelType.EMAIL)).thenReturn(false);
        when(renderer.render(eq("order-confirmed"), any(), any(), any())).thenReturn(new RenderedMail("Đã nhận đơn hàng ORD-1", "<p>x</p>"));

        boolean sent = dispatcher.dispatch(request("lan@example.com"));

        assertThat(sent).isTrue();
        ArgumentCaptor<OutboundMessage> msg = ArgumentCaptor.forClass(OutboundMessage.class);
        verify(email).send(msg.capture());
        assertThat(msg.getValue().recipient()).isEqualTo("lan@example.com");
        assertThat(msg.getValue().subject()).isEqualTo("Đã nhận đơn hàng ORD-1");
        ArgumentCaptor<NotificationLog> saved = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logs).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(NotificationLog.Status.SENT);
    }

    @Test
    void alreadyProcessed_isSkipped_withoutSending() {
        when(logs.existsByEventIdAndChannel(EVENT, ChannelType.EMAIL)).thenReturn(true);

        assertThat(dispatcher.dispatch(request("lan@example.com"))).isFalse();
        verifyNoInteractions(renderer);
        verify(email, never()).send(any());
        verify(logs, never()).save(any());
    }

    @Test
    void missingRecipient_isLoggedAsSkipped() {
        when(logs.existsByEventIdAndChannel(EVENT, ChannelType.EMAIL)).thenReturn(false);

        assertThat(dispatcher.dispatch(request(" "))).isTrue();
        ArgumentCaptor<NotificationLog> saved = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logs).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(NotificationLog.Status.SKIPPED);
        verify(email, never()).send(any());
    }

    @Test
    void channelFailure_propagates_andNothingLogged() {
        when(logs.existsByEventIdAndChannel(EVENT, ChannelType.EMAIL)).thenReturn(false);
        when(renderer.render(any(), any(), any(), any())).thenReturn(new RenderedMail("s", "b"));
        doThrow(new ChannelException("smtp down", null)).when(email).send(any());

        assertThatThrownBy(() -> dispatcher.dispatch(request("lan@example.com"))).isInstanceOf(ChannelException.class);
        verify(logs, never()).save(any());
    }

    private static DispatchRequest request(String recipient) {
        return new DispatchRequest(EVENT, "ORDER_CONFIRMED", ChannelType.EMAIL, recipient, "order-confirmed", Map.of("orderCode", "ORD-1"), "ORD-1");
    }
}
