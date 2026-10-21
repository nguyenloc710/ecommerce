package vn.ecommerce.notification.dispatch.internal;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.notification.NotificationProperties;
import vn.ecommerce.notification.channel.ChannelType;
import vn.ecommerce.notification.channel.NotificationChannel;
import vn.ecommerce.notification.channel.OutboundMessage;
import vn.ecommerce.notification.template.RenderedMail;
import vn.ecommerce.notification.template.TemplateRenderer;

/**
 * Thứ tự: kiểm tra log → render → gửi → ghi log. Gửi xong mà ghi log thất bại thì lần retry có thể gửi trùng;
 * chấp nhận với email (không chấp nhận với tiền), unique index (event_id, channel) chặn hai consumer song song.
 */
@Slf4j
@Service
class NotificationDispatcherImpl implements NotificationDispatcher {

    private final NotificationLogRepository logs;
    private final TemplateRenderer renderer;
    private final Map<ChannelType, NotificationChannel> channels;
    private final NotificationProperties properties;

    NotificationDispatcherImpl(NotificationLogRepository logs, TemplateRenderer renderer, List<NotificationChannel> channels,
                               NotificationProperties properties) {
        this.logs = logs;
        this.renderer = renderer;
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::type, Function.identity()));
        this.properties = properties;
    }

    @Override
    @Transactional
    public boolean dispatch(DispatchRequest request) {
        if (logs.existsByEventIdAndChannel(request.eventId(), request.channel())) {
            log.info("Event {} đã gửi qua {} trước đó, bỏ qua", request.eventId(), request.channel());
            return false;
        }
        if (request.recipient() == null || request.recipient().isBlank()) {
            logs.save(new NotificationLog(request.eventId(), request.kind(), request.channel(), "", null,
                    NotificationLog.Status.SKIPPED, "Thiếu người nhận"));
            log.warn("Event {} ({}) không có người nhận, bỏ qua", request.eventId(), request.kind());
            return true;
        }
        NotificationChannel channel = channels.get(request.channel());
        if (channel == null) {
            throw new IllegalStateException("Chưa cấu hình kênh " + request.channel());
        }
        RenderedMail mail = renderer.render(request.template(), properties.locale(), request.model(), request.subjectArgs());
        channel.send(new OutboundMessage(request.recipient(), mail.subject(), mail.html()));
        logs.save(new NotificationLog(request.eventId(), request.kind(), request.channel(), request.recipient(), mail.subject(),
                NotificationLog.Status.SENT, null));
        return true;
    }
}
