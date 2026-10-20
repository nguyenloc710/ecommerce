package vn.ecommerce.catalog.inventory.internal;

import java.util.UUID;
import vn.ecommerce.catalog.inventory.internal.dto.InventoryRow;
import vn.ecommerce.catalog.inventory.internal.dto.InventoryUpdateCommand;
import vn.ecommerce.catalog.product.PageView;

public interface StockAdminService {

    PageView<InventoryRow> page(String q, boolean lowStockOnly, int page, int size);

    /** Đặt số tồn tuyệt đối; tạo bản ghi tồn kho nếu SKU chưa có. */
    InventoryRow update(UUID skuId, InventoryUpdateCommand command);
}
