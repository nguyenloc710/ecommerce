package vn.ecommerce.catalog.product;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** API public của module product cho module khác tra cứu SKU theo lô (không N+1). */
public interface VariantLookupApi {

    /** Trả về các SKU tìm thấy (kể cả HIDDEN/DRAFT với {@code sellable=false}); SKU đã xóa mềm bị bỏ qua. */
    List<VariantInfo> findVariants(Collection<UUID> skuIds);

    /** Id các SKU có mã SKU hoặc tên sản phẩm chứa {@code q} (không phân biệt hoa thường), tối đa {@code limit}. */
    List<UUID> searchVariantIds(String q, int limit);
}
