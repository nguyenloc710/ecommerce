package vn.ecommerce.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRolesConverterTest {

    private final KeycloakRolesConverter converter = new KeycloakRolesConverter();

    @Test
    void convert_mapsRolesClaimToRolePrefixAndKeepsScopes() {
        Jwt jwt = jwtBuilder()
                .claim(KeycloakRolesConverter.ROLES_CLAIM, List.of("CUSTOMER", "STAFF"))
                .claim("scope", "openid email")
                .build();

        assertThat(authorities(jwt))
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_STAFF", "SCOPE_openid", "SCOPE_email");
    }

    @Test
    void convert_returnsOnlyScopes_whenRolesClaimMissing() {
        Jwt jwt = jwtBuilder().claim("scope", "openid").build();

        assertThat(authorities(jwt)).containsExactly("SCOPE_openid");
    }

    private List<String> authorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("user-1");
    }
}
