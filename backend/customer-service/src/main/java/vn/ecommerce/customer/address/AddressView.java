package vn.ecommerce.customer.address;

import java.util.UUID;

/** Địa chỉ giao hàng; tên tỉnh/phường là snapshot lúc lưu. */
public record AddressView(UUID id, String recipientName, String phone, String provinceCode, String provinceName,
                          String wardCode, String wardName, String line1, String note, boolean isDefault, long version) {

    /** Địa chỉ một dòng để hiển thị và snapshot vào đơn hàng. */
    public String fullText() {
        return String.join(", ", line1, wardName, provinceName);
    }
}
