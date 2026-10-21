package vn.ecommerce.order.checkout.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Dùng cho cả báo giá ({@code /api/checkout/quote}) và đặt hàng ({@code POST /api/orders}).
 * Giá không có trong request: luôn tra lại từ catalog. Địa chỉ: {@code addressId} (sổ địa chỉ) hoặc {@code address} nhập tay.
 *
 * @param paymentMethod  COD | MOCK_ONLINE (bắt buộc khi đặt, tùy chọn khi báo giá)
 * @param shippingCarrier mã hãng (GHN, GHTK); báo giá không có → trả mọi lựa chọn
 * @param clientIp IP trình duyệt khách do storefront chuyển tiếp (VNPay cần); chỉ dùng để gửi cổng thanh toán
 */
public record CheckoutRequest(
        @NotEmpty(message = "{validation.required}") @Size(max = 50, message = "{validation.max-length}") List<@Valid Line> lines,
        UUID addressId,
        @Valid AddressInput address,
        @Pattern(regexp = "COD|MOCK_ONLINE|VNPAY", message = "{validation.enum}") String paymentMethod,
        @Size(max = 20, message = "{validation.max-length}") String shippingCarrier,
        @Size(max = 30, message = "{validation.max-length}") String voucherCode,
        @Email(message = "{validation.email}") @Size(max = 255, message = "{validation.max-length}") String contactEmail,
        @Size(max = 500, message = "{validation.max-length}") String note,
        @Size(max = 45, message = "{validation.max-length}") String clientIp) {

    public record Line(
            @NotNull(message = "{validation.required}") UUID skuId,
            @Min(value = 1, message = "{validation.min}") @Max(value = 999, message = "{validation.max-length}") int quantity,
            @Size(max = 10, message = "{validation.max-length}") String engraving) {

        public String normalizedEngraving() {
            return engraving == null || engraving.isBlank() ? null : engraving.trim();
        }
    }

    /** Địa chỉ nhập tay (khách chưa lưu sổ địa chỉ); tên tỉnh/phường lấy từ {@code /api/divisions}. */
    public record AddressInput(
            @NotBlank(message = "{validation.required}") @Size(max = 150, message = "{validation.max-length}") String recipientName,
            @NotBlank(message = "{validation.required}") @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "{validation.phone}") String phone,
            @NotBlank(message = "{validation.required}") @Size(max = 10, message = "{validation.max-length}") String provinceCode,
            @NotBlank(message = "{validation.required}") @Size(max = 120, message = "{validation.max-length}") String provinceName,
            @NotBlank(message = "{validation.required}") @Size(max = 10, message = "{validation.max-length}") String wardCode,
            @NotBlank(message = "{validation.required}") @Size(max = 120, message = "{validation.max-length}") String wardName,
            @NotBlank(message = "{validation.required}") @Size(max = 255, message = "{validation.max-length}") String line1,
            @Size(max = 255, message = "{validation.max-length}") String note) {
    }
}
