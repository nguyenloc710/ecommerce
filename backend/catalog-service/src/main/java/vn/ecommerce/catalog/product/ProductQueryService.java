package vn.ecommerce.catalog.product;

import java.util.List;

/** API đọc công khai của module product (storefront). Chỉ trả dữ liệu đang bán, không lộ entity. */
public interface ProductQueryService {

    List<CategoryTreeNode> categoryTree();

    /** @throws vn.ecommerce.common.web.error.NotFoundException khi slug danh mục/thương hiệu không tồn tại */
    PageView<ProductCardView> listProducts(ProductListQuery query);

    /** Facet cho thanh lọc, tính trên tập khớp từ khóa/danh mục/thương hiệu (bỏ qua option và giá đang chọn). */
    FacetsView facets(ProductListQuery query);

    /** @throws vn.ecommerce.common.web.error.NotFoundException khi sản phẩm không tồn tại hoặc không ở trạng thái bán */
    ProductDetailView productDetail(String slug);
}
