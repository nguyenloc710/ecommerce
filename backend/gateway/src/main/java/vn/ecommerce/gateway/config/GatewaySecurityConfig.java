package vn.ecommerce.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import vn.ecommerce.common.security.KeycloakRolesConverter;
import vn.ecommerce.common.security.Roles;

/**
 * Phân quyền thô theo đường dẫn tại gateway. Service phía sau vẫn tự validate JWT và kiểm tra quyền
 * sở hữu tài nguyên (phòng thủ nhiều lớp). Token được chuyển nguyên xuống service.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    SecurityWebFilterChain gatewaySecurityFilterChain(ServerHttpSecurity http) {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> { })
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/divisions/**",
                                "/api/products/**", "/api/categories/**", "/api/brands/**", "/api/reviews/**", "/api/flash-sales/**", "/api/gold-rates/**", "/api/inventory/**")
                        .permitAll()
                        // Webhook đối tác: không có JWT, order-service kiểm tra chữ ký
                        .pathMatchers("/webhooks/**").permitAll()
                        .pathMatchers("/api/admin/**").hasAnyRole(Roles.STAFF, Roles.ADMIN)
                        // /internal/** không có route nào, chặn thêm ở đây cho chắc
                        .pathMatchers("/internal/**").denyAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtConverter))))
                .build();
    }
}
