package vn.ecommerce.order.orders.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import vn.ecommerce.order.shipping.ShipmentStatusChanged;

/** Nghe webhook hãng vận chuyển (qua module shipping) để chuyển đơn sang DELIVERED; chạy sau commit, transaction riêng. */
@Component
@RequiredArgsConstructor
class ShipmentStatusListener {

    private final OrderService orderService;

    @ApplicationModuleListener
    void on(ShipmentStatusChanged event) {
        orderService.onShipmentStatus(event);
    }
}
