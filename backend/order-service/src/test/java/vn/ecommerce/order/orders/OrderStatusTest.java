package vn.ecommerce.order.orders;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Bảng chuyển trạng thái B-04: mọi nhánh hợp lệ và các nhánh bị cấm quan trọng. */
class OrderStatusTest {

    @Test
    void happyPath_cod() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PACKING)).isTrue();
        assertThat(OrderStatus.PACKING.canTransitionTo(OrderStatus.SHIPPING)).isTrue();
        assertThat(OrderStatus.SHIPPING.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
    }

    @Test
    void onlinePayment_waitsThenConfirmsOrCancels() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.AWAITING_PAYMENT)).isTrue();
        assertThat(OrderStatus.AWAITING_PAYMENT.allowedNext()).containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
    }

    @Test
    void cannotCancel_onceShipped_orSkipSteps() {
        assertThat(OrderStatus.SHIPPING.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
        assertThat(OrderStatus.CANCELLED.isFinal()).isTrue();
        assertThat(OrderStatus.REFUNDED.isFinal()).isTrue();
    }

    @Test
    void customerCancellable_onlyBeforePacking() {
        assertThat(OrderStatus.PENDING.customerCancellable()).isTrue();
        assertThat(OrderStatus.AWAITING_PAYMENT.customerCancellable()).isTrue();
        assertThat(OrderStatus.CONFIRMED.customerCancellable()).isTrue();
        assertThat(OrderStatus.PACKING.customerCancellable()).isFalse();
        assertThat(OrderStatus.SHIPPING.customerCancellable()).isFalse();
    }

    @Test
    void returnFlow() {
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.RETURN_REQUESTED)).isTrue();
        assertThat(OrderStatus.RETURN_REQUESTED.canTransitionTo(OrderStatus.RETURNED)).isTrue();
        assertThat(OrderStatus.RETURNED.canTransitionTo(OrderStatus.REFUNDED)).isTrue();
    }
}
