package vn.ecommerce.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event gửi giữa các service qua RabbitMQ.
 * <p>
 * Quy tắc tiến hóa: chỉ thêm field mới (bên nhận bỏ qua field lạ); đổi nghĩa hoặc xóa field thì tạo
 * event mới với routing key {@code .v2}. Bên nhận dùng {@link #eventId()} để xử lý idempotent.
 */
public interface IntegrationEvent {

    UUID eventId();

    Instant occurredAt();
}
