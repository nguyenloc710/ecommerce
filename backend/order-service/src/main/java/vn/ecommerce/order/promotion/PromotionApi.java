package vn.ecommerce.order.promotion;

import java.math.BigDecimal;
import java.util.UUID;

/** API public của module promotion cho checkout. */
public interface PromotionApi {

    /**
     * Tính giảm giá của một mã cho khách và tạm tính hiện tại.
     *
     * @throws vn.ecommerce.common.web.error.BadRequestException {@code promotion.*} khi mã không áp dụng được
     */
    DiscountQuote quote(String code, UUID customerId, BigDecimal subtotal);

    /** Ghi lượt dùng khi đơn được tạo (cùng transaction với tạo đơn); tăng {@code used_count}. */
    void recordUsage(UUID voucherId, UUID orderId, UUID customerId, BigDecimal discount);
}
