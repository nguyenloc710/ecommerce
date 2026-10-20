package vn.ecommerce.common.web.error;

import java.util.List;

/** Trang kết quả chuẩn cho mọi service (B-05: {@code page} 0-based). Không phụ thuộc Spring Data để common-web nhẹ. */
public record PageResponse<T>(List<T> items, int page, int size, long total, boolean hasNext) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        return new PageResponse<>(items, page, size, total, (long) (page + 1) * size < total);
    }
}
