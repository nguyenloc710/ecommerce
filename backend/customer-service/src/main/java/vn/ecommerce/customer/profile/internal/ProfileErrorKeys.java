package vn.ecommerce.customer.profile.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProfileErrorKeys {

    public static final String NOT_FOUND = "customer.not-found";
    /** JWT không có email: tài khoản Keycloak lỗi cấu hình. */
    public static final String EMAIL_MISSING = "customer.email-missing";
}
