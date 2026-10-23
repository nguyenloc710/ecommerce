package vn.ecommerce.catalog.review.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.ecommerce.catalog.media.MediaApi;
import vn.ecommerce.catalog.messaging.ProcessedEvents;
import vn.ecommerce.catalog.product.VariantInfo;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.catalog.review.internal.ReviewDtos.CreateReviewCommand;
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

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final String CUSTOMER = "5f84289e-e54c-4683-86de-d001358572e3";
    private static final UUID PRODUCT = UUID.randomUUID();
    private static final UUID SKU = UUID.randomUUID();
    private static final UUID ORDER = UUID.randomUUID();
    private static final String KEY = "reviews/" + CUSTOMER + "/0f8fad5b-d9cb-469f-a165-70867728950e.jpg";

    @Mock ReviewRepository reviews;
    @Mock ReviewEligibilityRepository eligibilities;
    @Mock VariantLookupApi variants;
    @Mock MediaApi media;
    @Mock ProcessedEvents processedEvents;
    @Mock CurrentUser currentUser;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage(ReviewServiceImpl.ANONYMOUS_AUTHOR_KEY, java.util.Locale.forLanguageTag("vi"), "Khách hàng");
        service = new ReviewServiceImpl(reviews, eligibilities, variants, media, processedEvents, currentUser, new ReviewProperties(5, 10), messages);
        lenient().when(currentUser.requireId()).thenReturn(CUSTOMER);
        lenient().when(currentUser.id()).thenReturn(Optional.of(CUSTOMER));
        lenient().when(currentUser.displayName()).thenReturn(Optional.of("Nguyễn Thị Lan"));
        lenient().when(media.isReviewImageOf(eq(CUSTOMER), anyString())).thenAnswer(inv -> KEY.equals(inv.getArgument(1)));
    }

    @Test
    void create_eligibleCustomer_savesPendingReview_withShortenedAuthorName() {
        when(eligibilities.findFirstByCustomerIdAndProductIdOrderByDeliveredAtDesc(CUSTOMER, PRODUCT)).thenReturn(Optional.of(eligibility()));
        when(reviews.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(new CreateReviewCommand(PRODUCT, 5, "  Nhẫn đẹp, đeo vừa  ", List.of(KEY)));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviews).save(saved.capture());
        assertThat(saved.getValue().getAuthorName()).isEqualTo("Nguyễn L.");
        assertThat(saved.getValue().getContent()).isEqualTo("Nhẫn đẹp, đeo vừa");
        assertThat(saved.getValue().getOrderId()).isEqualTo(ORDER);
        assertThat(saved.getValue().getSkuId()).isEqualTo(SKU);
        assertThat(saved.getValue().getImageKeys()).containsExactly(KEY);
        assertThat(result.status()).isEqualTo("PENDING");
    }

    @Test
    void create_notPurchased_isForbidden_andSavesNothing() {
        when(eligibilities.findFirstByCustomerIdAndProductIdOrderByDeliveredAtDesc(CUSTOMER, PRODUCT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateReviewCommand(PRODUCT, 4, null, null)))
                .isInstanceOf(ForbiddenException.class).hasMessage(ReviewErrorKeys.NOT_ELIGIBLE);
        verify(reviews, never()).save(any());
    }

    @Test
    void create_secondReviewSameProduct_isConflict() {
        when(eligibilities.findFirstByCustomerIdAndProductIdOrderByDeliveredAtDesc(CUSTOMER, PRODUCT)).thenReturn(Optional.of(eligibility()));
        when(reviews.existsByCustomerIdAndProductId(CUSTOMER, PRODUCT)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateReviewCommand(PRODUCT, 4, "ok", List.of())))
                .isInstanceOf(ConflictException.class).hasMessage(ReviewErrorKeys.ALREADY_EXISTS);
        verify(reviews, never()).save(any());
    }

    @Test
    void create_imageOfAnotherCustomer_isRejected() {
        String foreign = "reviews/someone-else/0f8fad5b-d9cb-469f-a165-70867728950e.jpg";

        assertThatThrownBy(() -> service.create(new CreateReviewCommand(PRODUCT, 4, "ok", List.of(foreign))))
                .isInstanceOf(BadRequestException.class).hasMessage(ReviewErrorKeys.INVALID_IMAGE);
        verify(reviews, never()).save(any());
    }

    @Test
    void create_tooManyImages_isRejected() {
        List<String> six = List.of("a", "b", "c", "d", "e", "f");

        assertThatThrownBy(() -> service.create(new CreateReviewCommand(PRODUCT, 4, "ok", six)))
                .isInstanceOf(BadRequestException.class).hasMessage(ReviewErrorKeys.TOO_MANY_IMAGES);
    }

    @Test
    void eligibility_coversLoginPurchaseAndDuplicate() {
        when(currentUser.id()).thenReturn(Optional.empty());
        assertThat(service.eligibility(PRODUCT).reason()).isEqualTo(ReviewServiceImpl.REASON_LOGIN_REQUIRED);

        when(currentUser.id()).thenReturn(Optional.of(CUSTOMER));
        when(eligibilities.findFirstByCustomerIdAndProductIdOrderByDeliveredAtDesc(CUSTOMER, PRODUCT)).thenReturn(Optional.empty());
        assertThat(service.eligibility(PRODUCT).reason()).isEqualTo(ReviewServiceImpl.REASON_NOT_PURCHASED);

        when(eligibilities.findFirstByCustomerIdAndProductIdOrderByDeliveredAtDesc(CUSTOMER, PRODUCT)).thenReturn(Optional.of(eligibility()));
        assertThat(service.eligibility(PRODUCT)).extracting("canReview", "reason").containsExactly(true, ReviewServiceImpl.REASON_OK);

        when(reviews.existsByCustomerIdAndProductId(CUSTOMER, PRODUCT)).thenReturn(true);
        assertThat(service.eligibility(PRODUCT).reason()).isEqualTo(ReviewServiceImpl.REASON_ALREADY_REVIEWED);
    }

    @Test
    void recordDelivery_opensEligibilityPerSku_skipsExisting_andIsIdempotentPerEvent() {
        UUID event = UUID.randomUUID();
        UUID sku2 = UUID.randomUUID();
        when(processedEvents.markProcessed(event, ReviewServiceImpl.CONSUMER)).thenReturn(true, false);
        when(variants.findVariants(List.of(SKU, sku2))).thenReturn(List.of(variant(SKU), variant(sku2)));
        when(eligibilities.existsByOrderIdAndSkuId(ORDER, SKU)).thenReturn(true);

        assertThat(service.recordDelivery(event, ORDER, CUSTOMER, List.of(SKU, sku2), Instant.now())).isTrue();
        assertThat(service.recordDelivery(event, ORDER, CUSTOMER, List.of(SKU, sku2), Instant.now())).isFalse();

        ArgumentCaptor<ReviewEligibility> saved = ArgumentCaptor.forClass(ReviewEligibility.class);
        verify(eligibilities, times(1)).save(saved.capture());
        assertThat(saved.getValue().getSkuId()).isEqualTo(sku2);
        assertThat(saved.getValue().getProductId()).isEqualTo(PRODUCT);
    }

    @Test
    void productReviews_summarizesApprovedRatings() {
        when(reviews.findByProductIdAndStatusOrderByCreatedAtDesc(eq(PRODUCT), eq(ReviewStatus.APPROVED), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(reviews.countApprovedByRating(PRODUCT)).thenReturn(List.of(new RatingCount((short) 5, 3), new RatingCount((short) 4, 1)));

        var view = service.productReviews(PRODUCT, 0, 0);

        assertThat(view.totalCount()).isEqualTo(4);
        assertThat(view.averageRating()).isEqualTo(4.8);
        assertThat(view.countByRating()).containsExactlyInAnyOrderEntriesOf(Map.of(5, 3L, 4, 1L, 3, 0L, 2, 0L, 1, 0L));
    }

    private static ReviewEligibility eligibility() {
        return new ReviewEligibility(CUSTOMER, PRODUCT, SKU, ORDER, Instant.now());
    }

    private static VariantInfo variant(UUID sku) {
        return new VariantInfo(sku, PRODUCT, "nhan", "Nhẫn", "RNG", Map.of("size", "6"), BigDecimal.TEN, null, null, true);
    }
}
