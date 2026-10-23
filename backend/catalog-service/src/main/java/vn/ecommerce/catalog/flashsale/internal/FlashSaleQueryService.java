package vn.ecommerce.catalog.flashsale.internal;

import java.util.List;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CurrentFlashSale;

public interface FlashSaleQueryService {

    /** Các chương trình đang chạy (sắp kết thúc trước), chỉ gồm SKU còn bán được; rỗng nếu không có. */
    List<CurrentFlashSale> current();
}
