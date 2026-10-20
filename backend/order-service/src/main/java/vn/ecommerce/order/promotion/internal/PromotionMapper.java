package vn.ecommerce.order.promotion.internal;

import org.mapstruct.Mapper;
import vn.ecommerce.order.promotion.VoucherView;

@Mapper
public interface PromotionMapper {

    VoucherView toView(Voucher voucher);
}
