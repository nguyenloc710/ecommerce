package vn.ecommerce.catalog.media;

import java.time.Instant;
import java.util.Map;

/**
 * Vé upload: trình duyệt gửi {@code PUT uploadUrl} kèm đúng {@code headers} (Content-Type đã được ký), body là file ảnh.
 *
 * @param objectKey giữ lại để gửi kèm khi tạo đánh giá
 * @param publicUrl URL xem ảnh sau khi upload xong
 */
public record UploadTicket(String uploadUrl, String objectKey, String publicUrl, Map<String, String> headers, Instant expiresAt) {
}
