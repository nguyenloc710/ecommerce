package vn.ecommerce.catalog.product;

import java.math.BigDecimal;
import java.util.List;

/** Bộ lọc khả dụng cho thanh lọc PLP (UI-08): option của variant, thuộc tính sản phẩm, khoảng giá. */
public record FacetsView(List<Facet> options, List<Facet> attributes, BigDecimal priceMin, BigDecimal priceMax) {

    public record Facet(String name, List<Value> values) {
    }

    public record Value(String value, long count) {
    }
}
