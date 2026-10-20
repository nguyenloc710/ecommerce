package vn.ecommerce.catalog.product.internal.repo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kết quả đếm facet: {@code options} đếm theo option của variant (size, color...), {@code attributes} theo
 * thuộc tính sản phẩm (Chất liệu...). Map lồng giữ thứ tự xuất hiện.
 */
public record FacetCounts(Map<String, LinkedHashMap<String, Long>> options,
                          Map<String, LinkedHashMap<String, Long>> attributes,
                          BigDecimal priceMin, BigDecimal priceMax) {
}
