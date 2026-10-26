package vn.ecommerce.order.orders.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.order.orders.OrderStatus;
import vn.ecommerce.order.orders.PaymentOutcome;
import vn.ecommerce.order.shipping.ShippingApi;

/** Checklist go-live mục 17: thanh toán về trễ sau khi đơn đã hết hạn/hủy (B-21) và webhook lặp lại. */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orders;
    @Mock OrderCodeGenerator codes;
    @Mock OrderStateMachine stateMachine;
    @Mock OrderViewMapper mapper;
    @Mock ShippingApi shipping;
    @Mock CurrentUser currentUser;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orders, codes, stateMachine, mapper, shipping, new OrderProperties(null, null, null), currentUser);
    }

    private static Order onlineOrder(OrderStatus... path) {
        Order order = new Order(UUID.randomUUID(), "ORD-261023-0001", UUID.randomUUID(), "idem", new BigDecimal("300000"), BigDecimal.ZERO,
                new BigDecimal("25000"), null, "Lan", "0900000000", "lan@example.com", Map.of(), null, "VNPAY", "GHN", "system");
        for (OrderStatus s : path) {
            order.transitionTo(s, null, "system");
        }
        return order;
    }

    @Test
    void markPaid_awaitingPayment_confirmsOrder() {
        Order order = onlineOrder(OrderStatus.AWAITING_PAYMENT);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        service.markPaid(order.getId(), "TXN-1");

        verify(stateMachine).transition(order, OrderStatus.CONFIRMED, "paid:TXN-1");
        verify(orders).save(order);
    }

    @Test
    void markPaid_afterOrderExpiredAndCancelled_keepsCancelled_forManualRefund() {
        Order order = onlineOrder(OrderStatus.AWAITING_PAYMENT, OrderStatus.CANCELLED);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        assertThat(service.markPaid(order.getId(), "TXN-LATE")).isEqualTo(new PaymentOutcome("ORD-261023-0001", true));

        verify(stateMachine, never()).transition(any(), any(), any());
        verify(orders, never()).save(any());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void markPaid_alreadyConfirmed_isIdempotent() {
        Order order = onlineOrder(OrderStatus.AWAITING_PAYMENT, OrderStatus.CONFIRMED);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        assertThat(service.markPaid(order.getId(), "TXN-1").orderCancelled()).isFalse();

        verify(stateMachine, never()).transition(any(), any(), any());
        verify(orders, never()).save(any());
    }

    @Test
    void markPaymentFailed_onlyCancelsWhileAwaitingPayment() {
        Order confirmed = onlineOrder(OrderStatus.AWAITING_PAYMENT, OrderStatus.CONFIRMED);
        when(orders.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));

        service.markPaymentFailed(confirmed.getId(), "declined");

        verify(stateMachine, never()).transition(any(), any(), any());
    }
}
