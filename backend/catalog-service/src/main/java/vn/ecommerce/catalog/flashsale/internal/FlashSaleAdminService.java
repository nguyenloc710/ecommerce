package vn.ecommerce.catalog.flashsale.internal;

import java.util.UUID;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.CreateCommand;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.FlashSaleDetail;
import vn.ecommerce.catalog.flashsale.internal.FlashSaleDtos.FlashSaleRow;
import vn.ecommerce.common.web.error.PageResponse;

/** Nhân viên tạo, xem, dừng chương trình flash sale. Chương trình đã tạo không sửa giá/suất: dừng rồi tạo mới. */
public interface FlashSaleAdminService {

    PageResponse<FlashSaleRow> list(int page, int size);

    FlashSaleDetail detail(UUID id);

    FlashSaleDetail create(CreateCommand command);

    /** Dừng chương trình (sắp diễn ra hoặc đang chạy); đơn đã giữ suất vẫn giữ giá đã báo. */
    FlashSaleDetail cancel(UUID id, Long version);
}
