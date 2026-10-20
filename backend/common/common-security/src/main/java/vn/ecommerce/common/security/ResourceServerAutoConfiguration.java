package vn.ecommerce.common.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Security chain mặc định cho service servlet: stateless, chỉ nhận Bearer JWT của Keycloak.
 * <ul>
 *   <li>{@code /actuator/**}: mở, vì actuator chỉ truy cập được trong cluster (NetworkPolicy).</li>
 *   <li>{@code /internal/**}: chỉ service account có role {@link Roles#INTERNAL}.</li>
 *   <li>{@code /api/admin/**}: {@link Roles#STAFF} hoặc {@link Roles#ADMIN}.</li>
 *   <li>Còn lại: phải đăng nhập, trừ các path khai báo trong {@link SecurityProperties}.</li>
 * </ul>
 * Lỗi 401/403 trả body {@code ErrorResponse} như controller. Issuer, JWKS và audience cấu hình qua
 * {@code spring.security.oauth2.resourceserver.jwt.*}.
 */
// Chạy trước các auto-config tạo SecurityFilterChain mặc định của Boot để chain này được dùng.
@AutoConfiguration(beforeName = {
        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration",
        "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration"
})
@EnableMethodSecurity
// Tự bật WebSecurity để chain dùng được cả trong slice test (@WebMvcTest) nơi auto-config security của Boot không có mặt
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(SecurityProperties.class)
public class ResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CurrentUser currentUser() {
        return new CurrentUser();
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    SecurityErrorWriter securityErrorWriter(ObjectProvider<JsonMapper> jsonMapper, MessageSource messageSource) {
        // Dùng JsonMapper do Boot cấu hình (ngày giờ ISO-8601); không có thì tạo mặc định
        return new SecurityErrorWriter(jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()), messageSource);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain resourceServerFilterChain(HttpSecurity http, SecurityProperties properties,
                                                  JwtAuthenticationConverter jwtAuthenticationConverter,
                                                  SecurityErrorWriter errorWriter) throws Exception {
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(errorWriter);
        JsonAccessDeniedHandler accessDenied = new JsonAccessDeniedHandler(errorWriter);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll();
                    properties.publicPaths().forEach(path -> auth.requestMatchers(path).permitAll());
                    properties.publicGetPaths().forEach(path -> auth.requestMatchers(HttpMethod.GET, path).permitAll());
                    auth.requestMatchers("/internal/**").hasRole(Roles.INTERNAL);
                    auth.requestMatchers("/api/admin/**").hasAnyRole(Roles.STAFF, Roles.ADMIN);
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDenied))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
