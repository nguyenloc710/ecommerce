package vn.ecommerce.catalog.product;

import java.util.List;

/**
 * Trang kết quả cho storefront (B-05: {@code page} 0-based). {@code hasNext} phục vụ nút "Xem thêm" (UI-08).
 */
public record PageView<T>(List<T> items, int page, int size, long total, boolean hasNext) {
}
