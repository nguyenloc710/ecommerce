package vn.ecommerce.customer.staffadmin.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StaffAdminErrorKeys {

    public static final String KEYCLOAK_UNAVAILABLE = "staffadmin.keycloak-unavailable";
    public static final String USER_NOT_FOUND = "staffadmin.user-not-found";
    /** {0} = email. */
    public static final String EMAIL_TAKEN = "staffadmin.email-taken";
    public static final String CANNOT_LOCK_SELF = "staffadmin.cannot-lock-self";
}
