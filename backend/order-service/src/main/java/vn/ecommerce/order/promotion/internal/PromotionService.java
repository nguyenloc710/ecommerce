package vn.ecommerce.order.promotion.internal;

import java.util.UUID;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.promotion.DiscountQuote;
import vn.ecommerce.order.promotion.PromotionApi;
import vn.ecommerce.order.promotion.VoucherView;

public interface PromotionService extends PromotionApi {

    /** Khách đang đăng nhập kiểm tra mã. */
    DiscountQuote validateForCurrent(VoucherValidateCommand command);

    PageResponse<VoucherView> search(String q, int page, int size);

    VoucherView get(UUID id);

    VoucherView create(VoucherUpsertCommand command);

    VoucherView update(UUID id, VoucherUpsertCommand command);

    void delete(UUID id);
}
