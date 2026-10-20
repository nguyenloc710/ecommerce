package vn.ecommerce.catalog.product.internal.admin;

import java.util.UUID;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.internal.admin.dto.BrandAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.BrandUpsertCommand;

public interface BrandAdminService {

    PageView<BrandAdminView> list(int page, int size);

    BrandAdminView create(BrandUpsertCommand command);

    BrandAdminView update(UUID id, BrandUpsertCommand command);

    /** Xóa mềm; từ chối khi còn sản phẩm gắn thương hiệu. */
    void delete(UUID id);
}
