package vn.ecommerce.catalog.messaging;

import java.util.UUID;

/**
 * Sổ event đã xử lý theo từng consumer. Gọi <b>trong cùng transaction</b> với nghiệp vụ của consumer:
 * nếu trả {@code false} thì event đã xử lý rồi, bỏ qua; khóa chính (event_id, consumer) chặn xử lý trùng khi chạy song song.
 */
public interface ProcessedEvents {

    boolean markProcessed(UUID eventId, String consumer);
}
