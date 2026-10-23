package vn.ecommerce.catalog.flashsale.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleQuota.AcquireResult;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleQuota.Claim;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleQuota.Taken;

/** Script Lua chạy thật trên Valkey (cùng image với local/docker-compose.yml). */
@Testcontainers
class ValkeyFlashSaleQuotaTest {

    private static final Duration TTL = Duration.ofMinutes(5);

    @Container
    static final GenericContainer<?> VALKEY = new GenericContainer<>("public.ecr.aws/valkey/valkey:8.1.10-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private static ValkeyFlashSaleQuota quota;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(VALKEY.getHost(), VALKEY.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redis = new StringRedisTemplate(connectionFactory);
        quota = new ValkeyFlashSaleQuota(redis);
    }

    @AfterAll
    static void close() {
        connectionFactory.destroy();
    }

    @Test
    void acquire_initialisesFromDbValues_andDecrements() {
        UUID item = UUID.randomUUID();

        assertThat(quota.acquire("kh-1", List.of(new Claim(item, 2, 0, 5, 0, TTL))).ok()).isTrue();
        // Khóa đã có: giá trị DB truyền lần sau bị bỏ qua
        assertThat(quota.acquire("kh-2", List.of(new Claim(item, 3, 0, 99, 0, TTL))).ok()).isTrue();

        assertThat(redis.opsForValue().get("flash:{" + item + "}:remaining")).isEqualTo("0");
        assertThat(redis.getExpire("flash:{" + item + "}:remaining")).isPositive();
        AcquireResult soldOut = quota.acquire("kh-3", List.of(new Claim(item, 1, 0, 99, 0, TTL)));
        assertThat(soldOut.ok()).isFalse();
        assertThat(soldOut.rejectedItemId()).isEqualTo(item);
        assertThat(soldOut.limitExceeded()).isFalse();
    }

    @Test
    void acquire_isAllOrNothing_acrossItems() {
        UUID plenty = UUID.randomUUID();
        UUID scarce = UUID.randomUUID();

        AcquireResult result = quota.acquire("kh-1", List.of(new Claim(plenty, 1, 0, 10, 0, TTL), new Claim(scarce, 2, 0, 1, 0, TTL)));

        assertThat(result.rejectedItemId()).isEqualTo(scarce);
        assertThat(redis.opsForValue().get("flash:{" + plenty + "}:remaining")).isEqualTo("10");
        assertThat(redis.opsForValue().get("flash:{" + plenty + "}:cust:kh-1")).isEqualTo("0");
    }

    @Test
    void acquire_enforcesPerCustomerLimit_includingDbHistory() {
        UUID item = UUID.randomUUID();

        // Khách đã mua 1 theo DB, giới hạn 2: mua thêm 1 được, thêm 1 nữa thì vượt
        assertThat(quota.acquire("kh-1", List.of(new Claim(item, 1, 2, 10, 1, TTL))).ok()).isTrue();
        AcquireResult over = quota.acquire("kh-1", List.of(new Claim(item, 1, 2, 10, 1, TTL)));
        assertThat(over.limitExceeded()).isTrue();
        // Khách khác không bị ảnh hưởng
        assertThat(quota.acquire("kh-2", List.of(new Claim(item, 2, 2, 10, 0, TTL))).ok()).isTrue();
    }

    @Test
    void release_givesBackQuota_andSkipsMissingKeys() {
        UUID item = UUID.randomUUID();
        quota.acquire("kh-1", List.of(new Claim(item, 2, 3, 2, 0, TTL)));

        quota.release("kh-1", List.of(new Taken(item, 2)));

        assertThat(redis.opsForValue().get("flash:{" + item + "}:remaining")).isEqualTo("2");
        assertThat(redis.opsForValue().get("flash:{" + item + "}:cust:kh-1")).isEqualTo("0");
        UUID unknown = UUID.randomUUID();
        quota.release("kh-1", List.of(new Taken(unknown, 1)));
        assertThat(redis.hasKey("flash:{" + unknown + "}:remaining")).isFalse();
    }
}
