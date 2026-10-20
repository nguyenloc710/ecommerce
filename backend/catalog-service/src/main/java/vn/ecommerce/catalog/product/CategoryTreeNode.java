package vn.ecommerce.catalog.product;

import java.util.List;
import java.util.UUID;

/** Một nút trong cây danh mục công khai (chỉ danh mục ACTIVE). */
public record CategoryTreeNode(UUID id, String name, String slug, int sortOrder, List<CategoryTreeNode> children) {
}
