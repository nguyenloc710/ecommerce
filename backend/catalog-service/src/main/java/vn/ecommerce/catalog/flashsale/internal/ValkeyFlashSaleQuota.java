package vn.ecommerce.catalog.flashsale.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/** Bộ đếm suất trên Valkey; mỗi thao tác là một script Lua nên kiểm tra và trừ diễn ra nguyên tử. */
@Slf4j
@Component
@RequiredArgsConstructor
class ValkeyFlashSaleQuota implements FlashSaleQuota {

    private static final RedisScript<Long> ACQUIRE = RedisScript.of(new ClassPathResource("flashsale/acquire.lua"), Long.class);
    private static final RedisScript<Long> RELEASE = RedisScript.of(new ClassPathResource("flashsale/release.lua"), Long.class);

    private final StringRedisTemplate redis;

    @Override
    public AcquireResult acquire(String customerId, List<Claim> claims) {
        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();
        for (Claim c : claims) {
            keys.addAll(keysOf(c.itemId(), customerId));
            args.addAll(List.of(String.valueOf(c.quantity()), String.valueOf(c.perCustomerLimit()), String.valueOf(c.initialRemaining()),
                    String.valueOf(c.initialUsed()), String.valueOf(Math.max(1, c.ttl().toSeconds()))));
        }
        long code = execute(ACQUIRE, keys, args);
        if (code == 0) {
            return AcquireResult.OK;
        }
        return new AcquireResult(claims.get((int) Math.abs(code) - 1).itemId(), code < 0);
    }

    @Override
    public void release(String customerId, List<Taken> taken) {
        if (taken.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();
        for (Taken t : taken) {
            keys.addAll(keysOf(t.itemId(), customerId));
            args.add(String.valueOf(t.quantity()));
        }
        execute(RELEASE, keys, args);
    }

    private long execute(RedisScript<Long> script, List<String> keys, List<String> args) {
        try {
            Long result = redis.execute(script, keys, args.toArray());
            return result == null ? 0 : result;
        } catch (DataAccessException e) {
            log.warn("Valkey lỗi khi chạy script flash sale: {}", e.getMessage());
            throw new FlashSaleUnavailableException();
        }
    }

    private static List<String> keysOf(UUID itemId, String customerId) {
        return List.of(FlashSaleConstants.REMAINING_KEY.formatted(itemId),
                FlashSaleConstants.CUSTOMER_KEY.formatted(itemId, customerId == null ? FlashSaleConstants.ANONYMOUS_CUSTOMER : customerId));
    }
}
