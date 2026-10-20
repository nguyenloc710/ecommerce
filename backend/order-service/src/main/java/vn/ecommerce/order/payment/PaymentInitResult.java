package vn.ecommerce.order.payment;

import java.util.UUID;

/** @param payUrl null với COD */
public record PaymentInitResult(UUID paymentId, PaymentMethod method, String status, String payUrl) {
}
