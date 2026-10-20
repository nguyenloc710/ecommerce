package vn.ecommerce.customer.address;

/** Đơn vị hành chính (tỉnh/thành hoặc phường/xã) cho form chọn địa chỉ. */
public record DivisionView(String code, String name, String divisionType, String parentCode) {
}
