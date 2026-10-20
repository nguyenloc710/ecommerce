package vn.ecommerce.catalog.inventory.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nhả hàng của đơn quá hạn giữ. Chạy được trên nhiều replica cùng lúc: optimistic lock + kiểm tra trạng thái
 * trong transaction bảo đảm không nhả hai lần.
 */
@Component
@RequiredArgsConstructor
class ExpiredReservationJob {

    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${ecommerce.inventory.expiry-check-interval:60s}", initialDelayString = "30s")
    void releaseExpired() {
        reservationService.releaseExpired();
    }
}
