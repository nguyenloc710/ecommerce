package vn.ecommerce.notification.dispatch.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.notification.channel.ChannelType;

/** Một thông báo đã xử lý cho một event trên một kênh; bất biến. */
@Getter
@Entity
@Table(name = "notification_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 40)
    private String kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channel;

    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public NotificationLog(UUID eventId, String kind, ChannelType channel, String recipient, String subject, Status status, String detail) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.kind = kind;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.status = status;
        this.detail = detail;
        this.createdAt = Instant.now();
    }

    public enum Status {
        SENT,
        /** Không có người nhận (event thiếu email). */
        SKIPPED
    }
}
