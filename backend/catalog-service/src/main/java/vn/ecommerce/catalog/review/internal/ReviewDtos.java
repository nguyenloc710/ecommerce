package vn.ecommerce.catalog.review.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import vn.ecommerce.common.web.error.PageResponse;

/** Hợp đồng API của module review. */
public final class ReviewDtos {

    private ReviewDtos() {
    }

    /** Đánh giá hiển thị công khai (chỉ APPROVED); tên tác giả đã rút gọn khi tạo. */
    public record ReviewView(UUID id, int rating, String content, String authorName, String variantLabel, List<String> imageUrls,
                             Instant createdAt) {
    }

    /**
     * @param averageRating làm tròn 1 chữ số; 0 khi chưa có đánh giá
     * @param countByRating số đánh giá theo mức sao 1..5 (đủ 5 khóa)
     */
    public record ProductReviewsView(UUID productId, double averageRating, long totalCount, Map<Integer, Long> countByRating,
                                     PageResponse<ReviewView> reviews) {
    }

    /** @param reason LOGIN_REQUIRED | NOT_PURCHASED | ALREADY_REVIEWED | OK */
    public record EligibilityView(boolean canReview, String reason) {
    }

    /**
     * @param imageKeys object key nhận từ {@code POST /api/media/review-images}, tối đa {@code ecommerce.review.max-images}
     */
    public record CreateReviewCommand(
            @NotNull(message = "{validation.required}") UUID productId,
            @Min(value = 1, message = "{validation.min}") @Max(value = 5, message = "{validation.max}") int rating,
            @Size(max = 2000, message = "{validation.max-length}") String content,
            List<@NotBlank(message = "{validation.required}") String> imageKeys) {
    }

    /** Khách nhận lại trạng thái chờ duyệt. */
    public record SubmittedReview(UUID id, String status) {
    }

    public record AdminReviewRow(UUID id, UUID productId, String productName, String productSlug, String variantLabel, String authorName,
                                 int rating, String content, List<String> imageUrls, String status, String rejectReason, Instant createdAt,
                                 Instant moderatedAt) {
    }

    public record RejectCommand(@NotBlank(message = "{validation.required}") @Size(max = 255, message = "{validation.max-length}") String reason) {
    }
}
