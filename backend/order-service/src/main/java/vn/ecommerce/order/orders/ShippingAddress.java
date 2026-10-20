package vn.ecommerce.order.orders;

/** Snapshot địa chỉ giao hàng lưu trong đơn (JSONB); không đổi khi khách sửa sổ địa chỉ. */
public record ShippingAddress(String recipientName, String phone, String provinceCode, String provinceName,
                              String wardCode, String wardName, String line1, String note) {

    public String fullText() {
        return String.join(", ", line1, wardName, provinceName);
    }
}
