package vn.ecommerce.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/**
 * Checklist go-live mục 17: service từ chối token có {@code iss} hoặc {@code aud} sai, hoặc đã hết hạn. Dùng đúng bộ
 * property các service khai báo (issuer-uri public + jwk-set-uri nội bộ + audiences), token ký thật bằng khóa RSA phục vụ
 * qua JWKS cục bộ.
 */
class JwtValidationTest {

    private static final String ISSUER = "https://auth.example.vn/realms/ecommerce";
    private static final String AUDIENCE = "ecommerce-api";

    private static RSAKey key;
    private static HttpServer jwks;

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
                    OAuth2ResourceServerAutoConfiguration.class, ResourceServerAutoConfiguration.class))
            .withPropertyValues(
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + ISSUER,
                    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:" + jwks.getAddress().getPort() + "/certs",
                    "spring.security.oauth2.resourceserver.jwt.audiences=" + AUDIENCE);

    @BeforeAll
    static void startJwks() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwks = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        jwks.createContext("/certs", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        jwks.start();
    }

    @AfterAll
    static void stopJwks() {
        jwks.stop(0);
    }

    private static String token(String issuer, List<String> audience, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("user-1").issuer(issuer).audience(audience)
                .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(15)))).expirationTime(Date.from(expiresAt)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static Instant inFiveMinutes() {
        return Instant.now().plus(Duration.ofMinutes(5));
    }

    @Test
    void acceptsTokenWithRightIssuerAndAudience() {
        contextRunner.run(ctx -> {
            JwtDecoder decoder = ctx.getBean(JwtDecoder.class);
            assertThat(decoder.decode(token(ISSUER, List.of(AUDIENCE, "account"), inFiveMinutes())).getSubject()).isEqualTo("user-1");
        });
    }

    @Test
    void rejectsWrongAudience() {
        contextRunner.run(ctx -> {
            String wrongAud = token(ISSUER, List.of("account"), inFiveMinutes());
            assertThatThrownBy(() -> ctx.getBean(JwtDecoder.class).decode(wrongAud)).isInstanceOf(JwtValidationException.class)
                    .hasMessageContaining("aud");
        });
    }

    @Test
    void rejectsWrongIssuer() {
        contextRunner.run(ctx -> {
            String wrongIss = token("http://evil.example/realms/ecommerce", List.of(AUDIENCE), inFiveMinutes());
            assertThatThrownBy(() -> ctx.getBean(JwtDecoder.class).decode(wrongIss)).isInstanceOf(JwtValidationException.class)
                    .hasMessageContaining("iss");
        });
    }

    @Test
    void rejectsExpiredToken() {
        contextRunner.run(ctx -> {
            String expired = token(ISSUER, List.of(AUDIENCE), Instant.now().minus(Duration.ofMinutes(10)));
            assertThatThrownBy(() -> ctx.getBean(JwtDecoder.class).decode(expired)).isInstanceOf(JwtValidationException.class)
                    .hasMessageContaining("expired");
        });
    }
}
