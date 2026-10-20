package vn.ecommerce.order.checkout.internal;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.order.customerclient.CustomerClient;
import vn.ecommerce.order.orders.ShippingAddress;

/** Địa chỉ giao hàng: ưu tiên sổ địa chỉ (B-15, không tin client), không có thì địa chỉ nhập tay. */
@Component
@RequiredArgsConstructor
class AddressResolver {

    private final CustomerClient customers;

    Optional<ShippingAddress> resolve(UUID customerId, CheckoutRequest request) {
        if (request.addressId() != null) {
            return Optional.of(customers.findAddress(customerId, request.addressId())
                    .map(a -> new ShippingAddress(a.recipientName(), a.phone(), a.provinceCode(), a.provinceName(), a.wardCode(), a.wardName(), a.line1(), a.note()))
                    .orElseThrow(() -> new BadRequestException(CheckoutErrorKeys.ADDRESS_NOT_FOUND)));
        }
        CheckoutRequest.AddressInput in = request.address();
        if (in == null) {
            return Optional.empty();
        }
        return Optional.of(new ShippingAddress(in.recipientName().trim(), in.phone(), in.provinceCode(), in.provinceName(), in.wardCode(),
                in.wardName(), in.line1().trim(), in.note()));
    }

    ShippingAddress require(UUID customerId, CheckoutRequest request) {
        return resolve(customerId, request).orElseThrow(() -> new BadRequestException(CheckoutErrorKeys.ADDRESS_REQUIRED));
    }
}
