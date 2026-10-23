package vn.ecommerce.catalog.media.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Dev: tạo bucket ảnh nếu chưa có (SeaweedFS local). Prod (R2) tạo bucket + CORS + domain công khai bằng tay, tắt cờ này.
 * Kho ảnh chưa chạy thì chỉ cảnh báo, service vẫn khởi động.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ecommerce.media", name = "create-bucket-on-startup", havingValue = "true")
class DevBucketInitializer implements ApplicationRunner {

    private final MediaProperties props;
    private final S3Client s3;

    @Override
    public void run(ApplicationArguments args) {
        try {
            s3.headBucket(b -> b.bucket(props.bucket()));
        } catch (NoSuchBucketException e) {
            s3.createBucket(b -> b.bucket(props.bucket()));
            log.info("Đã tạo bucket ảnh {}", props.bucket());
        } catch (SdkException e) {
            log.warn("Không kiểm tra được bucket ảnh {} tại {}: {}", props.bucket(), props.endpoint(), e.getMessage());
        }
    }
}
