package vn.ecommerce.catalog.product.internal.admin;

import java.util.List;
import java.util.UUID;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryUpsertCommand;

public interface CategoryAdminService {

    /** Mọi danh mục (kể cả HIDDEN), phẳng, có parentId để admin dựng cây. */
    List<CategoryAdminView> list();

    CategoryAdminView create(CategoryUpsertCommand command);

    CategoryAdminView update(UUID id, CategoryUpsertCommand command);

    /** Xóa mềm; từ chối khi còn danh mục con hoặc sản phẩm. */
    void delete(UUID id);
}
