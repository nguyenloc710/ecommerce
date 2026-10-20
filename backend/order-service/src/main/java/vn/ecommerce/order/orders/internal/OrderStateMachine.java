package vn.ecommerce.order.orders.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import vn.ecommerce.common.events.order.OrderCancelledEvent;
import vn.ecommerce.common.events.order.OrderConfirmedEvent;
import vn.ecommerce.common.events.order.OrderDeliveredEvent;
import vn.ecommerce.common.events.order.OrderShippedEvent;
import vn.ecommerce.common.jpa.CurrentAuditorProvider;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.order.orders.OrderStatus;

/**
 * Điểm duy nhất đổi trạng thái đơn: kiểm tra bảng chuyển, ghi lịch sử, phát event tích hợp tương ứng.
 * Gọi trong transaction của service; event đi qua outbox của Modulith nên chỉ phát khi commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class OrderStateMachine {

    private final ApplicationEventPublisher events;
    private final CurrentUser currentUser;

    void transition(Order order, OrderStatus target, String reason) {
        transition(order, target, reason, null, null);
    }

    void transition(Order order, OrderStatus target, String reason, String carrier, String trackingCode) {
        OrderStatus from = order.getStatus();
        order.transitionTo(target, reason, actor());
        log.info("Đơn {}: {} → {}{}", order.getCode(), from, target, reason == null ? "" : " (" + reason + ")");
        switch (target) {
            case CONFIRMED -> {
                order.markPaidAt(isOnline(order) ? Instant.now() : null);
                events.publishEvent(new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(), order.getId(), order.getCode(),
                        order.getCustomerId().toString(), order.getContactEmail(), order.getTotal(), confirmedLines(order)));
            }
            case CANCELLED -> events.publishEvent(new OrderCancelledEvent(UUID.randomUUID(), Instant.now(), order.getId(),
                    order.getCode(), order.getCustomerId().toString(), order.getContactEmail(), reason));
            case SHIPPING -> events.publishEvent(new OrderShippedEvent(UUID.randomUUID(), Instant.now(), order.getId(),
                    order.getCode(), order.getCustomerId().toString(), order.getContactEmail(), carrier, trackingCode));
            case DELIVERED -> events.publishEvent(new OrderDeliveredEvent(UUID.randomUUID(), Instant.now(), order.getId(),
                    order.getCode(), order.getCustomerId().toString(), order.getContactEmail()));
            default -> {
                // trạng thái khác không có event tích hợp
            }
        }
    }

    String actor() {
        return currentUser.id().orElse(CurrentAuditorProvider.SYSTEM);
    }

    private static boolean isOnline(Order order) {
        return !"COD".equals(order.getPaymentMethod());
    }

    private static List<OrderConfirmedEvent.Line> confirmedLines(Order order) {
        return order.getItems().stream().map(i -> new OrderConfirmedEvent.Line(i.getSkuId(), i.getProductName(), i.getVariantLabel(),
                i.getQuantity(), i.getUnitPrice())).toList();
    }
}
