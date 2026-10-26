package vn.ecommerce.order.payment.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import vn.ecommerce.common.events.order.OrderCancelledEvent;

/** Đơn bị hủy sau khi đã thanh toán online → mở yêu cầu hoàn tiền (B-33); chạy sau commit, transaction riêng. */
@Component
@RequiredArgsConstructor
class OrderCancelledRefundListener {

    private final RefundService refunds;

    @ApplicationModuleListener
    void on(OrderCancelledEvent event) {
        refunds.openForCancelledOrder(event.orderId(), event.orderCode(), event.reason());
    }
}
