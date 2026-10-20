package vn.ecommerce.customer.address.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình module address.
 *
 * @param maxPerCustomer số địa chỉ tối đa một khách được lưu
 */
@ConfigurationProperties(prefix = "ecommerce.address")
public record AddressProperties(Integer maxPerCustomer) {

    public AddressProperties {
        maxPerCustomer = maxPerCustomer == null ? 10 : maxPerCustomer;
    }
}
