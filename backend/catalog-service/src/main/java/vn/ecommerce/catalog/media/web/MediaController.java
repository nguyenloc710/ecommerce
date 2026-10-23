package vn.ecommerce.catalog.media.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.media.MediaApi;
import vn.ecommerce.catalog.media.UploadTicket;
import vn.ecommerce.common.security.CurrentUser;

@RestController
@RequiredArgsConstructor
@Tag(name = "Ảnh")
class MediaController {

    private final MediaApi media;
    private final CurrentUser currentUser;

    @PostMapping("/api/media/review-images")
    @Operation(summary = "Xin URL upload ảnh đánh giá",
            description = "Trả URL ký sẵn (hết hạn sau ít phút); trình duyệt PUT file kèm đúng header Content-Type, rồi gửi `objectKey` khi tạo đánh giá.")
    UploadTicket reviewImage(@Valid @RequestBody UploadRequest request) {
        return media.presignReviewImage(currentUser.requireId(), request.contentType(), request.size());
    }

    record UploadRequest(@NotBlank(message = "{validation.required}") String contentType,
                         @Positive(message = "{validation.min}") long size) {
    }
}
