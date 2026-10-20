package vn.ecommerce.catalog.product.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.ProductCardView;
import vn.ecommerce.catalog.product.ProductDetailView;
import vn.ecommerce.catalog.product.ProductListQuery;
import vn.ecommerce.catalog.product.ProductQueryService;
import vn.ecommerce.catalog.product.ProductSort;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Sản phẩm (công khai)")
class ProductController {

    private static final int DEFAULT_PAGE_SIZE = 24;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductQueryService productQueryService;

    @GetMapping
    @Operation(summary = "Danh sách sản phẩm đang bán",
            description = "Lọc theo danh mục (gồm danh mục con) và thương hiệu. 404 `category.not-found` / `brand.not-found` khi slug không tồn tại. "
                    + "`sort`: newest (mặc định) | price_asc | price_desc. `page` 0-based, `size` tối đa 100.")
    PageView<ProductCardView> list(
            @Parameter(description = "Slug danh mục") @RequestParam(required = false) String category,
            @Parameter(description = "Slug thương hiệu") @RequestParam(required = false) String brand,
            @RequestParam(required = false) @Pattern(regexp = "(?i)newest|price_asc|price_desc", message = "{validation.product.sort}") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        ProductSort productSort = ProductSort.fromParam(sort).orElse(ProductSort.NEWEST);
        return productQueryService.listProducts(new ProductListQuery(category, brand, productSort, page, size));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Chi tiết sản phẩm theo slug", description = "404 `product.not-found` nếu không tồn tại hoặc không ở trạng thái bán.")
    ProductDetailView detail(@PathVariable String slug) {
        return productQueryService.productDetail(slug);
    }
}
