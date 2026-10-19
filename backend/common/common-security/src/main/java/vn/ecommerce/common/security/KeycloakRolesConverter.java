package vn.ecommerce.common.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Đổi claim {@code roles} (mapper "realm-roles-flat" của Keycloak) thành {@code ROLE_*},
 * giữ thêm {@code SCOPE_*} từ claim {@code scope}. Dùng chung cho service servlet và gateway reactive.
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.hasClaim(ROLES_CLAIM) ? jwt.getClaimAsStringList(ROLES_CLAIM) : List.of();
        Stream<GrantedAuthority> roleAuthorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role));
        return Stream.concat(roleAuthorities, scopeConverter.convert(jwt).stream()).toList();
    }
}
