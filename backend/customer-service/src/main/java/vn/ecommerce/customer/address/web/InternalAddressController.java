package vn.ecommerce.customer.address.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.customer.address.AddressView;
import vn.ecommerce.customer.address.internal.AddressService;

/** Cho order-service (role INTERNAL) lấy địa chỉ giao hàng đáng tin cậy khi checkout. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Nội bộ: địa chỉ")
class InternalAddressController {

    private final AddressService addressService;

    @GetMapping("/internal/customers/{customerId}/addresses/{addressId}")
    @Operation(summary = "Địa chỉ của một khách", description = "404 nếu địa chỉ không thuộc khách hoặc đã xóa.")
    AddressView get(@PathVariable UUID customerId, @PathVariable UUID addressId) {
        return addressService.getForCustomer(customerId, addressId);
    }
}
