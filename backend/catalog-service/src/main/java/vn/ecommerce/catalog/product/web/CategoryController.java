package vn.ecommerce.catalog.product.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.product.CategoryTreeNode;
import vn.ecommerce.catalog.product.ProductQueryService;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Danh mục (công khai)")
class CategoryController {

    private final ProductQueryService productQueryService;

    @GetMapping
    @Operation(summary = "Cây danh mục đang hiển thị", description = "Chỉ gồm danh mục ACTIVE, sắp theo sort_order rồi tên. Không cần đăng nhập.")
    List<CategoryTreeNode> tree() {
        return productQueryService.categoryTree();
    }
}
