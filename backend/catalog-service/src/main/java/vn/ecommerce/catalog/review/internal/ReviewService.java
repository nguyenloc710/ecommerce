package vn.ecommerce.catalog.review.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import vn.ecommerce.catalog.review.internal.ReviewDtos.CreateReviewCommand;
import vn.ecommerce.catalog.review.internal.ReviewDtos.EligibilityView;
import vn.ecommerce.catalog.review.internal.ReviewDtos.ProductReviewsView;
import vn.ecommerce.catalog.review.internal.ReviewDtos.SubmittedReview;

/** Đánh giá phía khách: đọc công khai, kiểm tra quyền, gửi đánh giá, mở quyền khi đơn giao xong. */
public interface ReviewService {

    ProductReviewsView productReviews(UUID productId, int page, int size);

    /** Không ném lỗi khi chưa đăng nhập: trả {@code LOGIN_REQUIRED} để trang sản phẩm hiện nút đăng nhập. */
    EligibilityView eligibility(UUID productId);

    /**
     * @throws vn.ecommerce.common.web.error.ForbiddenException {@code review.not-eligible}
     * @throws vn.ecommerce.common.web.error.ConflictException  {@code review.already-exists}
     * @throws vn.ecommerce.common.web.error.BadRequestException {@code review.too-many-images}, {@code review.invalid-image}
     */
    SubmittedReview create(CreateReviewCommand command);

    /**
     * Mở quyền đánh giá cho các SKU của đơn đã giao; idempotent theo event và (order, sku).
     *
     * @return false nếu event đã xử lý trước đó
     */
    boolean recordDelivery(UUID eventId, UUID orderId, String customerId, Collection<UUID> skuIds, Instant deliveredAt);
}
