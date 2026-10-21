package vn.ecommerce.notification.dispatch.internal;

/** Render + gửi + ghi log trong một transaction; idempotent theo (eventId, channel). */
public interface NotificationDispatcher {

    /**
     * @return {@code true} nếu đã gửi (hoặc bỏ qua vì thiếu người nhận) trong lần gọi này; {@code false} nếu event đã xử lý trước đó
     * @throws vn.ecommerce.notification.channel.ChannelException kênh gửi lỗi → consumer retry rồi DLQ
     */
    boolean dispatch(DispatchRequest request);
}
