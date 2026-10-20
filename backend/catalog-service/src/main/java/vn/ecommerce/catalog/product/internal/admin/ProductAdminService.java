package vn.ecommerce.catalog.product.internal.admin;

import java.util.List;
import java.util.UUID;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.internal.admin.dto.ImageItemCommand;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminQuery;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminRow;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductUpsertCommand;
import vn.ecommerce.catalog.product.internal.admin.dto.VariantUpsertCommand;

/** Quản trị sản phẩm, biến thể, ảnh. Mọi thay đổi product/variant được Envers ghi lịch sử. */
public interface ProductAdminService {

    PageView<ProductAdminRow> page(ProductAdminQuery query);

    ProductAdminView get(UUID productId);

    ProductAdminView create(ProductUpsertCommand command);

    ProductAdminView update(UUID productId, ProductUpsertCommand command);

    /** Xóa mềm sản phẩm và toàn bộ variant của nó. */
    void delete(UUID productId);

    ProductAdminView addVariant(UUID productId, VariantUpsertCommand command);

    ProductAdminView updateVariant(UUID productId, UUID variantId, VariantUpsertCommand command);

    ProductAdminView deleteVariant(UUID productId, UUID variantId);

    /** Thay toàn bộ danh sách ảnh theo thứ tự gửi lên; ảnh không còn trong danh sách bị xóa cứng. */
    ProductAdminView replaceImages(UUID productId, List<ImageItemCommand> images);
}
