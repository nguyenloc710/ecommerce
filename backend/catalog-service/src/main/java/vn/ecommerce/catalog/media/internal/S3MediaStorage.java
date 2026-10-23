package vn.ecommerce.catalog.media.internal;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import vn.ecommerce.catalog.media.MediaApi;
import vn.ecommerce.catalog.media.UploadTicket;
import vn.ecommerce.common.web.error.BadRequestException;

/**
 * Ảnh đánh giá: key {@code reviews/<customerId>/<uuid>.<đuôi>}. URL ký sẵn khóa cả Content-Type và Content-Length,
 * nên khách không đổi được loại/dung lượng file sau khi được cấp vé.
 */
@Component
@RequiredArgsConstructor
class S3MediaStorage implements MediaApi {

    static final String REVIEW_PREFIX = "reviews/";
    private static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private static final Pattern KEY_TAIL = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");

    private final MediaProperties props;
    private final S3Presigner presigner;

    @Override
    public UploadTicket presignReviewImage(String customerId, String contentType, long sizeBytes) {
        String type = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if (!props.allowedImageTypes().contains(type) || !EXTENSIONS.containsKey(type)) {
            throw new BadRequestException(MediaErrorKeys.TYPE_NOT_ALLOWED, contentType);
        }
        if (sizeBytes <= 0 || sizeBytes > props.maxReviewImageSize().toBytes()) {
            throw new BadRequestException(MediaErrorKeys.TOO_LARGE, props.maxReviewImageSize().toMegabytes());
        }
        String key = REVIEW_PREFIX + customerId + "/" + UUID.randomUUID() + "." + EXTENSIONS.get(type);
        PresignedPutObjectRequest presigned = presigner.presignPutObject(p -> p
                .signatureDuration(props.uploadExpiry())
                .putObjectRequest(PutObjectRequest.builder().bucket(props.bucket()).key(key).contentType(type).contentLength(sizeBytes).build()));
        return new UploadTicket(presigned.url().toString(), key, publicUrl(key), Map.of("Content-Type", type),
                Instant.now().plus(props.uploadExpiry()));
    }

    @Override
    public boolean isReviewImageOf(String customerId, String objectKey) {
        String prefix = REVIEW_PREFIX + customerId + "/";
        return objectKey != null && objectKey.startsWith(prefix) && KEY_TAIL.matcher(objectKey.substring(prefix.length())).matches();
    }

    @Override
    public String publicUrl(String objectKey) {
        String base = props.publicBaseUrl().endsWith("/") ? props.publicBaseUrl() : props.publicBaseUrl() + "/";
        return base + objectKey;
    }
}
