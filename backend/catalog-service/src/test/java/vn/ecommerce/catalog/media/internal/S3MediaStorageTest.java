package vn.ecommerce.catalog.media.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import vn.ecommerce.common.web.error.BadRequestException;

/** Presigner thật (ký cục bộ, không gọi mạng) để kiểm tra URL, key và giới hạn file. */
class S3MediaStorageTest {

    private static final String CUSTOMER = "5f84289e-e54c-4683-86de-d001358572e3";

    private final MediaProperties props = new MediaProperties("http://localhost:8333", "auto", "ecommerce-media", "k", "s",
            "https://cdn.example.vn/", true, false, Duration.ofMinutes(10), DataSize.ofMegabytes(5),
            List.of("image/jpeg", "image/png", "image/webp"));
    private final S3Presigner presigner = new MediaConfig().mediaPresigner(props);
    private final S3MediaStorage storage = new S3MediaStorage(props, presigner);

    @AfterEach
    void close() {
        presigner.close();
    }

    @Test
    void presign_putsUnderCustomerPrefix_signsContentType_andReturnsPublicUrl() {
        var ticket = storage.presignReviewImage(CUSTOMER, "IMAGE/JPEG", 200_000);

        assertThat(ticket.objectKey()).startsWith("reviews/" + CUSTOMER + "/").endsWith(".jpg");
        URI url = URI.create(ticket.uploadUrl());
        assertThat(url.getPath()).isEqualTo("/ecommerce-media/" + ticket.objectKey());
        assertThat(url.getQuery()).contains("X-Amz-Signature=").contains("X-Amz-Expires=600").contains("content-type");
        assertThat(ticket.headers()).containsEntry("Content-Type", "image/jpeg");
        assertThat(ticket.publicUrl()).isEqualTo("https://cdn.example.vn/" + ticket.objectKey());
        assertThat(storage.isReviewImageOf(CUSTOMER, ticket.objectKey())).isTrue();
    }

    @Test
    void presign_rejectsWrongTypeAndOversizedFile() {
        assertThatThrownBy(() -> storage.presignReviewImage(CUSTOMER, "image/gif", 1000)).isInstanceOf(BadRequestException.class)
                .hasMessage(MediaErrorKeys.TYPE_NOT_ALLOWED);
        assertThatThrownBy(() -> storage.presignReviewImage(CUSTOMER, "image/png", DataSize.ofMegabytes(6).toBytes()))
                .isInstanceOf(BadRequestException.class).hasMessage(MediaErrorKeys.TOO_LARGE);
    }

    @Test
    void ownership_rejectsOtherPrefixesAndPathTricks() {
        assertThat(storage.isReviewImageOf(CUSTOMER, "reviews/other/0f8fad5b-d9cb-469f-a165-70867728950e.jpg")).isFalse();
        assertThat(storage.isReviewImageOf(CUSTOMER, "reviews/" + CUSTOMER + "/../x/0f8fad5b-d9cb-469f-a165-70867728950e.jpg")).isFalse();
        assertThat(storage.isReviewImageOf(CUSTOMER, "products/0f8fad5b-d9cb-469f-a165-70867728950e.jpg")).isFalse();
    }
}
