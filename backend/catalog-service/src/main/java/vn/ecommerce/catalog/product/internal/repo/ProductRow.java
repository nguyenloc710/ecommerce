package vn.ecommerce.catalog.product.internal.repo;

import java.time.Instant;
import java.util.UUID;

/** Projection cho trang danh sách; giá và ảnh tra riêng theo lô để không N+1. */
public record ProductRow(UUID id, String slug, String name, Instant publishedAt) {
}
