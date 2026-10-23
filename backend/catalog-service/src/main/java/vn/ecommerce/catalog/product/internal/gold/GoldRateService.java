package vn.ecommerce.catalog.product.internal.gold;

import java.util.List;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.GoldRateView;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.SetRateCommand;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.SetRateResult;

public interface GoldRateService {

    /** Giá hiện hành của từng loại vàng đã có giá (công khai, hiển thị cách tính trên trang sản phẩm). */
    List<GoldRateView> current();

    /** Lịch sử nhập giá, mới nhất trước; {@code karat} null = mọi loại. */
    PageView<GoldRateView> history(String karat, int page, int size);

    /** Nhập giá mới (hiệu lực ngay) và tính lại giá mọi variant GOLD_RATE dùng loại vàng đó, trong cùng transaction. */
    SetRateResult setRate(SetRateCommand command);
}
