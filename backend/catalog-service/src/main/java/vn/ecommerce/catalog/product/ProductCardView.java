package vn.ecommerce.catalog.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Thẻ sản phẩm trên lưới (UI-07).
 *
 * @param price          giá thấp nhất trong các variant đang bán
 * @param compareAtPrice giá gốc của variant có giá thấp nhất, null nếu không giảm
 * @param imageUrl       ảnh đầu tiên theo {@code sort_order}, null nếu chưa có ảnh
 * @param colors         tên màu của các variant (distinct, theo thứ tự variant)
 * @param label          mã nhãn nhỏ góc ảnh ({@code NEW} khi mới đăng; sau này {@code BESTSELLER}, {@code SOLD_OUT}), FE dịch; null nếu không có
 */
public record ProductCardView(UUID id, String slug, String name, BigDecimal price, BigDecimal compareAtPrice,
                              String imageUrl, List<String> colors, String label) {
}
