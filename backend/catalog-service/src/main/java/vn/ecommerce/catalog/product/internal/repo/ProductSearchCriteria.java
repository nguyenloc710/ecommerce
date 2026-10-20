package vn.ecommerce.catalog.product.internal.repo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import vn.ecommerce.catalog.product.ProductSort;

/**
 * Điều kiện lọc danh sách sản phẩm đang bán.
 *
 * @param q           từ khóa (null/blank = không tìm); so khớp full-text không dấu hoặc chứa trong tên
 * @param categoryIds danh mục (đã gồm con cháu); rỗng = tất cả
 * @param brandId     null = tất cả
 * @param options     lọc theo option của variant, ví dụ {size: [M, L], color: [Đen]}: cùng key là OR, khác key là AND
 * @param priceMin    giá variant tối thiểu (null = bỏ qua)
 * @param priceMax    giá variant tối đa (null = bỏ qua)
 */
public record ProductSearchCriteria(String q, Set<UUID> categoryIds, UUID brandId, Map<String, List<String>> options,
                                    BigDecimal priceMin, BigDecimal priceMax, ProductSort sort) {

    public boolean hasQuery() {
        return q != null && !q.isBlank();
    }
}
