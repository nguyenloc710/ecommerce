package vn.ecommerce.order.customerclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Bản sao {@code AddressView} của customer-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerAddress(UUID id, String recipientName, String phone, String provinceCode, String provinceName,
                              String wardCode, String wardName, String line1, String note) {
}
