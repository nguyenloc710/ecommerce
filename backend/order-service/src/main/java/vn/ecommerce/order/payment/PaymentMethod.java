package vn.ecommerce.order.payment;

import java.util.Arrays;
import java.util.Optional;

/** Phương thức thanh toán; thêm cổng mới = thêm hằng + một {@code PaymentGateway} bean. */
public enum PaymentMethod {
    /** Thanh toán khi nhận hàng: đơn xác nhận ngay. */
    COD("cod", false),
    /** Cổng giả để test luồng online + webhook trước khi có VNPay/MoMo (B-18). */
    MOCK_ONLINE("mock", true),
    /** VNPay (E6.1): chỉ có bean khi bật {@code ecommerce.payment.vnpay.enabled}. */
    VNPAY("vnpay", true);

    private final String slug;
    private final boolean online;

    PaymentMethod(String slug, boolean online) {
        this.slug = slug;
        this.online = online;
    }

    /** Đoạn đường dẫn webhook: {@code /webhooks/payments/{slug}}. */
    public String slug() {
        return slug;
    }

    public boolean online() {
        return online;
    }

    public static Optional<PaymentMethod> fromSlug(String slug) {
        return Arrays.stream(values()).filter(m -> m.slug.equalsIgnoreCase(slug)).findFirst();
    }
}
