package vn.ecommerce.notification.channel;

/** Gửi thất bại ở hạ tầng kênh (SMTP không kết nối được, nhà cung cấp trả 5xx...). */
public class ChannelException extends RuntimeException {

    public ChannelException(String message, Throwable cause) {
        super(message, cause);
    }
}
