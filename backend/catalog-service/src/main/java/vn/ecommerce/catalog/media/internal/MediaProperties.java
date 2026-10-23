package vn.ecommerce.catalog.media.internal;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Kho ảnh S3-compatible: Cloudflare R2 ở prod (region {@code auto}), SeaweedFS ở local.
 *
 * @param publicBaseUrl        gốc URL công khai của bucket (CDN/domain R2), ghép với object key
 * @param pathStyle            SeaweedFS cần path-style; R2 chạy được cả hai
 * @param createBucketOnStartup chỉ bật ở dev: tạo bucket nếu chưa có
 * @param uploadExpiry         thời hạn URL ký sẵn
 * @param maxReviewImageSize   giới hạn mỗi ảnh đánh giá (ký vào Content-Length)
 */
@ConfigurationProperties(prefix = "ecommerce.media")
public record MediaProperties(String endpoint, String region, String bucket, String accessKey, String secretKey, String publicBaseUrl,
                              boolean pathStyle, boolean createBucketOnStartup, Duration uploadExpiry, DataSize maxReviewImageSize,
                              List<String> allowedImageTypes) {
}
