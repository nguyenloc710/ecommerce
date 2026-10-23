package vn.ecommerce.catalog.review.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.media.MediaApi;
import vn.ecommerce.catalog.messaging.ProcessedEvents;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.catalog.review.internal.ReviewDtos.CreateReviewCommand;
import vn.ecommerce.catalog.review.internal.ReviewDtos.EligibilityView;
import vn.ecommerce.catalog.review.internal.ReviewDtos.ProductReviewsView;
import vn.ecommerce.catalog.review.internal.ReviewDtos.ReviewView;
import vn.ecommerce.catalog.review.internal.ReviewDtos.SubmittedReview;
import vn.ecommerce.catalog.review.internal.domain.RatingCount;
import vn.ecommerce.catalog.review.internal.domain.Review;
import vn.ecommerce.catalog.review.internal.domain.ReviewEligibility;
import vn.ecommerce.catalog.review.internal.domain.ReviewEligibilityRepository;
import vn.ecommerce.catalog.review.internal.domain.ReviewRepository;
import vn.ecommerce.catalog.review.internal.domain.ReviewStatus;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.ForbiddenException;
import vn.ecommerce.common.web.error.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class ReviewServiceImpl implements ReviewService {

    static final String CONSUMER = "catalog.review";
    static final String REASON_OK = "OK";
    static final String REASON_LOGIN_REQUIRED = "LOGIN_REQUIRED";
    static final String REASON_NOT_PURCHASED = "NOT_PURCHASED";
    static final String REASON_ALREADY_REVIEWED = "ALREADY_REVIEWED";
    private static final int MAX_PAGE_SIZE = 50;
    static final String ANONYMOUS_AUTHOR_KEY = "review.anonymous-author";
    /** Tên tác giả lưu một lần lúc tạo, hiển thị cho mọi khách: dùng ngôn ngữ mặc định của shop. */
    private static final Locale STORE_LOCALE = Locale.forLanguageTag("vi");

    private final ReviewRepository reviews;
    private final ReviewEligibilityRepository eligibilities;
    private final VariantLookupApi variants;
    private final MediaApi media;
    private final ProcessedEvents processedEvents;
    private final CurrentUser currentUser;
    private final ReviewProperties properties;
    private final MessageSource messages;

    @Override
    @Transactional(readOnly = true)
    public ProductReviewsView productReviews(UUID productId, int page, int size) {
        int pageSize = size <= 0 ? properties.defaultPageSize() : Math.min(size, MAX_PAGE_SIZE);
        Page<Review> found = reviews.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED,
                PageRequest.of(Math.max(page, 0), pageSize));
        Map<UUID, VariantInfo> skus = lookup(found.getContent().stream().map(Review::getSkuId).toList());
        List<ReviewView> items = found.getContent().stream().map(r -> toView(r, skus.get(r.getSkuId()))).toList();

        Map<Integer, Long> byRating = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            byRating.put(star, 0L);
        }
        long total = 0;
        long sum = 0;
        for (RatingCount c : reviews.countApprovedByRating(productId)) {
            byRating.put((int) c.rating(), c.count());
            total += c.count();
            sum += c.rating() * c.count();
        }
        double average = total == 0 ? 0 : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue();
        return new ProductReviewsView(productId, average, total, byRating,
                PageResponse.of(items, found.getNumber(), found.getSize(), found.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public EligibilityView eligibility(UUID productId) {
        return currentUser.id()
                .map(customerId -> eligibilityOf(customerId, productId))
                .orElse(new EligibilityView(false, REASON_LOGIN_REQUIRED));
    }

    private EligibilityView eligibilityOf(String customerId, UUID productId) {
        if (reviews.existsByCustomerIdAndProductId(customerId, productId)) {
            return new EligibilityView(false, REASON_ALREADY_REVIEWED);
        }
        boolean purchased = eligibilities.findFirstByCustomerIdAndProductIdOrderByDeliveredAtDesc(customerId, productId).isPresent();
        return new EligibilityView(purchased, purchased ? REASON_OK : REASON_NOT_PURCHASED);
    }

    @Override
    public SubmittedReview create(CreateReviewCommand command) {
        String customerId = currentUser.requireId();
        List<String> imageKeys = command.imageKeys() == null ? List.of() : command.imageKeys().stream().distinct().toList();
        if (imageKeys.size() > properties.maxImages()) {
            throw new BadRequestException(ReviewErrorKeys.TOO_MANY_IMAGES, properties.maxImages());
        }
        if (imageKeys.stream().anyMatch(key -> !media.isReviewImageOf(customerId, key))) {
            throw new BadRequestException(ReviewErrorKeys.INVALID_IMAGE);
        }
        ReviewEligibility eligibility = eligibilities.findFirstByCustomerIdAndProductIdOrderByDeliveredAtDesc(customerId, command.productId())
                .orElseThrow(() -> new ForbiddenException(ReviewErrorKeys.NOT_ELIGIBLE));
        if (reviews.existsByCustomerIdAndProductId(customerId, command.productId())) {
            throw new ConflictException(ReviewErrorKeys.ALREADY_EXISTS);
        }
        String content = command.content() == null || command.content().isBlank() ? null : command.content().trim();
        Review review = reviews.save(new Review(eligibility, authorName(), command.rating(), content, imageKeys));
        log.info("Khách gửi đánh giá {} cho sản phẩm {} ({} sao, {} ảnh), chờ duyệt", review.getId(), command.productId(), command.rating(),
                imageKeys.size());
        return new SubmittedReview(review.getId(), review.getStatus().name());
    }

    @Override
    public boolean recordDelivery(UUID eventId, UUID orderId, String customerId, Collection<UUID> skuIds, Instant deliveredAt) {
        if (!processedEvents.markProcessed(eventId, CONSUMER)) {
            return false;
        }
        // SKU đã bị xóa khỏi catalog thì bỏ qua: không còn trang sản phẩm để đánh giá
        for (VariantInfo sku : variants.findVariants(skuIds)) {
            if (!eligibilities.existsByOrderIdAndSkuId(orderId, sku.skuId())) {
                eligibilities.save(new ReviewEligibility(customerId, sku.productId(), sku.skuId(), orderId, deliveredAt));
            }
        }
        return true;
    }

    private ReviewView toView(Review r, VariantInfo sku) {
        return new ReviewView(r.getId(), r.getRating(), r.getContent(), r.getAuthorName(), sku == null ? null : sku.variantLabel(),
                r.getImageKeys().stream().map(media::publicUrl).toList(), r.getCreatedAt());
    }

    private Map<UUID, VariantInfo> lookup(Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        return variants.findVariants(skuIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(VariantInfo::skuId, Function.identity()));
    }

    /** Tên hiển thị rút gọn để không lộ họ tên đầy đủ: "Nguyễn Thị Lan" → "Nguyễn L.". */
    String authorName() {
        String name = currentUser.displayName().orElse("").trim();
        String[] parts = name.split("\\s+");
        if (name.isEmpty()) {
            return messages.getMessage(ANONYMOUS_AUTHOR_KEY, null, STORE_LOCALE);
        }
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
