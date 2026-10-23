package vn.ecommerce.catalog.review.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.review.internal.ReviewDtos.CreateReviewCommand;
import vn.ecommerce.catalog.review.internal.ReviewDtos.EligibilityView;
import vn.ecommerce.catalog.review.internal.ReviewDtos.ProductReviewsView;
import vn.ecommerce.catalog.review.internal.ReviewDtos.SubmittedReview;
import vn.ecommerce.catalog.review.internal.ReviewService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Đánh giá")
class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/reviews")
    @Operation(summary = "Đánh giá đã duyệt của một sản phẩm", description = "Công khai. Kèm điểm trung bình và số đánh giá theo từng mức sao.")
    ProductReviewsView productReviews(@RequestParam UUID productId, @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "0") int size) {
        return reviewService.productReviews(productId, page, size);
    }

    @GetMapping("/api/reviews/eligibility")
    @Operation(summary = "Khách hiện tại có được đánh giá sản phẩm không",
            description = "Không cần đăng nhập: chưa đăng nhập trả `LOGIN_REQUIRED`. Các lý do khác: `NOT_PURCHASED`, `ALREADY_REVIEWED`, `OK`.")
    EligibilityView eligibility(@RequestParam UUID productId) {
        return reviewService.eligibility(productId);
    }

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gửi đánh giá", description = "Chỉ khách đã nhận hàng (B-10); đánh giá ở trạng thái PENDING tới khi nhân viên duyệt.")
    SubmittedReview create(@Valid @RequestBody CreateReviewCommand command) {
        return reviewService.create(command);
    }
}
