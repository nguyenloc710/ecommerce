package vn.ecommerce.catalog.review.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReviewErrorKeys {

    public static final String NOT_ELIGIBLE = "review.not-eligible";
    public static final String ALREADY_EXISTS = "review.already-exists";
    public static final String NOT_FOUND = "review.not-found";
    /** {0} = số ảnh tối đa. */
    public static final String TOO_MANY_IMAGES = "review.too-many-images";
    public static final String INVALID_IMAGE = "review.invalid-image";
    public static final String REJECT_REASON_REQUIRED = "review.reject-reason-required";
}
