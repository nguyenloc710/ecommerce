package vn.ecommerce.catalog.review.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.media.MediaApi;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.catalog.review.internal.ReviewDtos.AdminReviewRow;
import vn.ecommerce.catalog.review.internal.domain.Review;
import vn.ecommerce.catalog.review.internal.domain.ReviewRepository;
import vn.ecommerce.catalog.review.internal.domain.ReviewStatus;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.common.web.error.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class ReviewAdminServiceImpl implements ReviewAdminService {

    static final String TARGET = "REVIEW";
    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviews;
    private final VariantLookupApi variants;
    private final MediaApi media;
    private final AdminAuditRecorder audit;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReviewRow> list(String status, int page, int size) {
        ReviewStatus filter = parseStatus(status);
        Page<Review> found = reviews.findByStatusOrderByCreatedAtAsc(filter, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
        Map<UUID, VariantInfo> skus = lookup(found.getContent().stream().map(Review::getSkuId).toList());
        List<AdminReviewRow> rows = found.getContent().stream().map(r -> toRow(r, skus.get(r.getSkuId()))).toList();
        return PageResponse.of(rows, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    @Override
    public AdminReviewRow approve(UUID reviewId) {
        return moderate(reviewId, ReviewStatus.APPROVED, null);
    }

    @Override
    public AdminReviewRow reject(UUID reviewId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException(ReviewErrorKeys.REJECT_REASON_REQUIRED);
        }
        return moderate(reviewId, ReviewStatus.REJECTED, reason.trim());
    }

    private AdminReviewRow moderate(UUID reviewId, ReviewStatus target, String reason) {
        Review review = reviews.findById(reviewId).orElseThrow(() -> new NotFoundException(ReviewErrorKeys.NOT_FOUND));
        ReviewStatus before = review.getStatus();
        if (review.moderate(target, reason, currentUser.requireId())) {
            reviews.save(review);
            audit.record(target == ReviewStatus.APPROVED ? "REVIEW_APPROVE" : "REVIEW_REJECT", TARGET, reviewId.toString(),
                    reason == null ? Map.of("from", before.name(), "to", target.name())
                            : Map.of("from", before.name(), "to", target.name(), "reason", reason));
            log.info("Đánh giá {}: {} → {}", reviewId, before, target);
        }
        return toRow(review, lookup(List.of(review.getSkuId())).get(review.getSkuId()));
    }

    private static ReviewStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return ReviewStatus.PENDING;
        }
        try {
            return ReviewStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("validation.enum");
        }
    }

    private AdminReviewRow toRow(Review r, VariantInfo sku) {
        return new AdminReviewRow(r.getId(), r.getProductId(), sku == null ? null : sku.productName(), sku == null ? null : sku.productSlug(),
                sku == null ? null : sku.variantLabel(), r.getAuthorName(), r.getRating(), r.getContent(),
                r.getImageKeys().stream().map(media::publicUrl).toList(), r.getStatus().name(), r.getRejectReason(), r.getCreatedAt(),
                r.getModeratedAt());
    }

    private Map<UUID, VariantInfo> lookup(Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        return variants.findVariants(skuIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(VariantInfo::skuId, Function.identity()));
    }
}
