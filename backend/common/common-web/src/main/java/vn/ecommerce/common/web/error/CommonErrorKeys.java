package vn.ecommerce.common.web.error;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Message key dùng chung, nội dung nằm trong {@code common-messages*.properties}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonErrorKeys {

    public static final String VALIDATION_FAILED = "common.validation-failed";
    public static final String MALFORMED_REQUEST = "common.malformed-request";
    public static final String ACCESS_DENIED = "common.access-denied";
    public static final String UNAUTHORIZED = "common.unauthorized";
    public static final String CONCURRENT_MODIFICATION = "common.concurrent-modification";
    public static final String VERSION_REQUIRED = "common.version-required";
    public static final String INTERNAL_ERROR = "common.internal-error";
}
