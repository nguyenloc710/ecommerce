package vn.ecommerce.common.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Realm role của Keycloak (realm {@code ecommerce}), xem docs/ke-hoach-thiet-ke.md mục 5.2. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Roles {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String STAFF = "STAFF";
    public static final String ADMIN = "ADMIN";
    /** Gán cho service account (client credentials) được phép gọi {@code /internal/**}. */
    public static final String INTERNAL = "INTERNAL";
}
