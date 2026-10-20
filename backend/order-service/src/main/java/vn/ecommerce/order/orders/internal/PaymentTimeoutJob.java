package vn.ecommerce.order.orders.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** B-20: hủy đơn online quá hạn thanh toán; catalog nhả hàng qua event order.cancelled. */
@Slf4j
@Component
@RequiredArgsConstructor
class PaymentTimeoutJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${ecommerce.orders.expiry-check-interval:60s}", initialDelayString = "30s")
    void run() {
        int cancelled = orderService.cancelExpiredAwaitingPayment();
        if (cancelled > 0) {
            log.info("Đã hủy {} đơn quá hạn thanh toán", cancelled);
        }
    }
}
