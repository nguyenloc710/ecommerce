package vn.ecommerce.catalog.product.internal.admin.dto;

import java.util.UUID;

/**
 * Lọc bảng sản phẩm admin.
 *
 * @param q          tìm theo tên hoặc slug (chứa, không phân biệt hoa thường)
 * @param status     DRAFT|ACTIVE|HIDDEN, null = tất cả
 * @param categoryId lọc đúng danh mục (không gồm con), null = tất cả
 */
public record ProductAdminQuery(String q, String status, UUID categoryId, int page, int size) {
}
