package vn.ecommerce.catalog.product;

/**
 * Tham số trang danh sách sản phẩm.
 *
 * @param categorySlug lọc theo danh mục và toàn bộ danh mục con; null = tất cả
 * @param brandSlug    lọc theo thương hiệu; null = tất cả
 */
public record ProductListQuery(String categorySlug, String brandSlug, ProductSort sort, int page, int size) {
}
