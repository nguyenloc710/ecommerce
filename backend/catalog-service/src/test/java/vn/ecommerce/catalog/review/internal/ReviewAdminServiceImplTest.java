package vn.ecommerce.catalog.review.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.catalog.media.MediaApi;
import vn.ecommerce.catalog.product.VariantLookupApi;
import vn.ecommerce.catalog.review.internal.domain.Review;
import vn.ecommerce.catalog.review.internal.domain.ReviewEligibility;
import vn.ecommerce.catalog.review.internal.domain.ReviewRepository;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ReviewAdminServiceImplTest {

    private static final UUID REVIEW = UUID.randomUUID();

    @Mock ReviewRepository reviews;
    @Mock VariantLookupApi variants;
    @Mock MediaApi media;
    @Mock AdminAuditRecorder audit;
    @Mock CurrentUser currentUser;
    @InjectMocks ReviewAdminServiceImpl service;

    private Review review;

    @BeforeEach
    void setUp() {
        review = new Review(new ReviewEligibility("c-1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now()),
                "Lan N.", 5, "Đẹp", List.of());
        lenient().when(currentUser.requireId()).thenReturn("staff-1");
        lenient().when(variants.findVariants(any())).thenReturn(List.of());
    }

    @Test
    void approve_pendingReview_changesStatus_andAudits_onceEvenIfRepeated() {
        when(reviews.findById(REVIEW)).thenReturn(Optional.of(review));

        assertThat(service.approve(REVIEW).status()).isEqualTo("APPROVED");
        service.approve(REVIEW);

        verify(audit, times(1)).record(eq("REVIEW_APPROVE"), eq("REVIEW"), eq(REVIEW.toString()), anyMap());
        verify(reviews, times(1)).save(review);
    }

    @Test
    void reject_requiresReason_andHidesApprovedReview() {
        assertThatThrownBy(() -> service.reject(REVIEW, " ")).isInstanceOf(BadRequestException.class)
                .hasMessage(ReviewErrorKeys.REJECT_REASON_REQUIRED);
        verify(reviews, never()).findById(any());

        when(reviews.findById(REVIEW)).thenReturn(Optional.of(review));
        service.approve(REVIEW);
        var row = service.reject(REVIEW, "Nội dung quảng cáo");

        assertThat(row.status()).isEqualTo("REJECTED");
        assertThat(row.rejectReason()).isEqualTo("Nội dung quảng cáo");
        verify(audit).record(eq("REVIEW_REJECT"), eq("REVIEW"), anyString(), anyMap());
    }

    @Test
    void moderate_unknownReview_isNotFound() {
        when(reviews.findById(REVIEW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(REVIEW)).isInstanceOf(NotFoundException.class).hasMessage(ReviewErrorKeys.NOT_FOUND);
        verify(audit, never()).record(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void list_unknownStatus_isBadRequest() {
        assertThatThrownBy(() -> service.list("DELETED", 0, 20)).isInstanceOf(BadRequestException.class);
    }
}
