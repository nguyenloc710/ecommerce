package vn.ecommerce.notification.channel;

/**
 * Strategy cho một kênh gửi. Lỗi gửi ném {@link ChannelException} để consumer retry rồi đưa vào DLQ.
 */
public interface NotificationChannel {

    ChannelType type();

    void send(OutboundMessage message);
}
