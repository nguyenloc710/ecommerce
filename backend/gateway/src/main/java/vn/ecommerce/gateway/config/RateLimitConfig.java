package vn.ecommerce.gateway.config;

import java.net.InetSocketAddress;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/** Khóa rate limit: theo user khi đã đăng nhập, theo IP thật của khách khi chưa. */
@Configuration
public class RateLimitConfig {

    /** Cloudflare gắn IP thật của khách vào header này; VPS chỉ nhận traffic từ Cloudflare. */
    private static final String CLIENT_IP_HEADER = "CF-Connecting-IP";
    private static final String UNKNOWN = "unknown";

    @Bean
    @Primary
    KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> "user:" + principal.getName())
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + clientIp(exchange.getRequest())));
    }

    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("ip:" + clientIp(exchange.getRequest()));
    }

    private static String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst(CLIENT_IP_HEADER);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded;
        }
        return Optional.ofNullable(request.getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .orElse(UNKNOWN);
    }
}
