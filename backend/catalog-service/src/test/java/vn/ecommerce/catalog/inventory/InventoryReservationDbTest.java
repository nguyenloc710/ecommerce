package vn.ecommerce.catalog.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.ecommerce.catalog.inventory.internal.ReservationService;
import vn.ecommerce.catalog.inventory.internal.StockAdminService;
import vn.ecommerce.catalog.inventory.internal.dto.InventoryUpdateCommand;
import vn.ecommerce.catalog.inventory.internal.dto.ReservationResult;
import vn.ecommerce.catalog.inventory.internal.dto.ReserveRequest;
import vn.ecommerce.common.web.error.ConflictException;

/**
 * Chạy cả ứng dụng trên PostgreSQL thật (dữ liệu seed dev): hai đơn tranh chiếc nhẫn cuối cùng cùng lúc,
 * chỉ một bên giữ được; không bán âm kho.
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.modulith.events.externalization.enabled=false"
})
class InventoryReservationDbTest {

    /** Nhẫn moissanite, Bạc, size 6 trong R__seed_dev.sql. */
    private static final UUID SKU_RING_6 = UUID.fromString("40000000-0000-0000-0000-000000000702");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired ReservationService reservationService;
    @Autowired StockAdminService stockAdminService;

    @Test
    void twoOrdersRacingForLastUnit_onlyOneWins_andCommitDeductsStock() throws Exception {
        stockAdminService.update(SKU_RING_6, new InventoryUpdateCommand(1, 3, null));
        UUID orderA = UUID.randomUUID();
        UUID orderB = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> a = pool.submit(() -> race(start, orderA));
            Future<Object> b = pool.submit(() -> race(start, orderB));
            start.countDown();
            Object ra = a.get(30, TimeUnit.SECONDS);
            Object rb = b.get(30, TimeUnit.SECONDS);

            long wins = List.of(ra, rb).stream().filter(ReservationResult.class::isInstance).count();
            long conflicts = List.of(ra, rb).stream().filter(ConflictException.class::isInstance).count();
            assertThat(wins).as("đúng một đơn giữ được").isEqualTo(1);
            assertThat(conflicts).as("đơn kia bị từ chối").isEqualTo(1);

            UUID winner = ra instanceof ReservationResult ? orderA : orderB;
            ReservationResult committed = reservationService.commit(winner);
            assertThat(committed.status()).isEqualTo("COMMITTED");
            var row = stockAdminService.page("RNG-MS5-SL-6", false, 0, 10).items().get(0);
            assertThat(row.onHand()).isZero();
            assertThat(row.reserved()).isZero();
            assertThat(row.available()).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    private Object race(CountDownLatch start, UUID orderId) throws InterruptedException {
        start.await();
        try {
            return reservationService.reserve(orderId, List.of(new ReserveRequest.Line(SKU_RING_6, 1)));
        } catch (ConflictException ex) {
            return ex;
        }
    }

    @Test
    void release_returnsStock_andRepeatedReleaseIsNoop() {
        UUID sku = UUID.fromString("40000000-0000-0000-0000-000000000801");
        stockAdminService.update(sku, new InventoryUpdateCommand(4, 3, null));
        UUID order = UUID.randomUUID();

        reservationService.reserve(order, List.of(new ReserveRequest.Line(sku, 3)));
        assertThat(stockAdminService.page("RNG-PL2-SL-6", false, 0, 10).items().get(0).available()).isEqualTo(1);

        reservationService.release(order);
        reservationService.release(order);
        var row = stockAdminService.page("RNG-PL2-SL-6", false, 0, 10).items().get(0);
        assertThat(row.available()).isEqualTo(4);
        assertThat(row.reserved()).isZero();
    }
}
