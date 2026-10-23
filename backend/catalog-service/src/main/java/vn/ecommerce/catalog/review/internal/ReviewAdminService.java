package vn.ecommerce.catalog.review.internal;

import java.util.UUID;
import vn.ecommerce.catalog.review.internal.ReviewDtos.AdminReviewRow;
import vn.ecommerce.common.web.error.PageResponse;

/** Kiểm duyệt đánh giá (STAFF/ADMIN); mọi thay đổi ghi admin_audit_log. */
public interface ReviewAdminService {

    PageResponse<AdminReviewRow> list(String status, int page, int size);

    /** @throws vn.ecommerce.common.web.error.NotFoundException {@code review.not-found} */
    AdminReviewRow approve(UUID reviewId);

    /** Từ chối đánh giá chờ duyệt hoặc ẩn đánh giá đã duyệt; bắt buộc lý do. */
    AdminReviewRow reject(UUID reviewId, String reason);
}
