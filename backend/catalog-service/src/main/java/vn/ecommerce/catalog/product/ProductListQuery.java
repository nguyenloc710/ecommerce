package vn.ecommerce.catalog.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Tham số trang danh sách / tìm kiếm sản phẩm.
 *
 * @param categorySlug lọc theo danh mục và toàn bộ danh mục con; null = tất cả
 * @param brandSlug    lọc theo thương hiệu; null = tất cả
 * @param q            từ khóa tìm kiếm không dấu; null = không tìm
 * @param options      lọc option variant {size: [M, L]}: cùng key OR, khác key AND
 * @param priceMin     giá tối thiểu (VND), null = bỏ qua
 * @param priceMax     giá tối đa (VND), null = bỏ qua
 */
public record ProductListQuery(String categorySlug, String brandSlug, String q, Map<String, List<String>> options,
                               BigDecimal priceMin, BigDecimal priceMax, ProductSort sort, int page, int size) {

    public static ProductListQuery simple(String categorySlug, String brandSlug, ProductSort sort, int page, int size) {
        return new ProductListQuery(categorySlug, brandSlug, null, Map.of(), null, null, sort, page, size);
    }
}
