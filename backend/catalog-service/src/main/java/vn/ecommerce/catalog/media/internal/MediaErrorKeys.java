package vn.ecommerce.catalog.media.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MediaErrorKeys {

    /** {0} = kiểu file gửi lên. */
    public static final String TYPE_NOT_ALLOWED = "media.type-not-allowed";
    /** {0} = dung lượng tối đa (MB). */
    public static final String TOO_LARGE = "media.too-large";
}
