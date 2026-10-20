package vn.ecommerce.catalog.product.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.product.FacetsView;
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
    /** Tiền tố tham số lọc option variant: {@code ?opt.size=M,L&opt.color=Đen}. */
    private static final String OPTION_PREFIX = "opt.";
    private static final int MAX_OPTION_KEYS = 10;
    private static final int MAX_OPTION_VALUES = 20;

    private final ProductQueryService productQueryService;

    @GetMapping
    @Operation(summary = "Danh sách / tìm kiếm sản phẩm đang bán",
            description = "Lọc theo danh mục (gồm danh mục con), thương hiệu, từ khóa không dấu `q`, option variant `opt.<tên>=v1,v2` "
                    + "(cùng tên là HOẶC, khác tên là VÀ), khoảng giá `priceMin`/`priceMax`. "
                    + "`sort`: newest (mặc định; có `q` thì theo độ liên quan) | price_asc | price_desc. `page` 0-based, `size` tối đa 100. "
                    + "404 `category.not-found` / `brand.not-found` khi slug không tồn tại.")
    PageView<ProductCardView> list(
            @Parameter(description = "Slug danh mục") @RequestParam(required = false) String category,
            @Parameter(description = "Slug thương hiệu") @RequestParam(required = false) String brand,
            @Parameter(description = "Từ khóa, không cần dấu") @RequestParam(required = false) @Size(max = 100, message = "{validation.max-length}") String q,
            @RequestParam(required = false) @DecimalMin(value = "0", message = "{validation.min}") BigDecimal priceMin,
            @RequestParam(required = false) @DecimalMin(value = "0", message = "{validation.min}") BigDecimal priceMax,
            @RequestParam(required = false) @Pattern(regexp = "(?i)newest|price_asc|price_desc", message = "{validation.product.sort}") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> allParams) {
        ProductSort productSort = ProductSort.fromParam(sort).orElse(ProductSort.NEWEST);
        return productQueryService.listProducts(new ProductListQuery(category, brand, q, optionFilters(allParams), priceMin, priceMax, productSort, page, size));
    }

    @GetMapping("/facets")
    @Operation(summary = "Bộ lọc khả dụng cho thanh lọc", description = "Đếm số sản phẩm theo từng option/thuộc tính và khoảng giá trên tập khớp `category`/`brand`/`q`.")
    FacetsView facets(@RequestParam(required = false) String category,
                      @RequestParam(required = false) String brand,
                      @RequestParam(required = false) @Size(max = 100, message = "{validation.max-length}") String q) {
        return productQueryService.facets(new ProductListQuery(category, brand, q, Map.of(), null, null, ProductSort.NEWEST, 0, 1));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Chi tiết sản phẩm theo slug", description = "404 `product.not-found` nếu không tồn tại hoặc không ở trạng thái bán.")
    ProductDetailView detail(@PathVariable String slug) {
        return productQueryService.productDetail(slug);
    }

    /** Gom {@code opt.*=a,b} thành {size: [a, b]}; giới hạn số key/giá trị để tránh query quá lớn. */
    private static Map<String, List<String>> optionFilters(MultiValueMap<String, String> params) {
        Map<String, List<String>> options = new LinkedHashMap<>();
        params.forEach((key, values) -> {
            if (!key.startsWith(OPTION_PREFIX) || options.size() >= MAX_OPTION_KEYS) {
                return;
            }
            List<String> split = values.stream()
                    .flatMap(v -> Arrays.stream(v.split(",")))
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .distinct()
                    .limit(MAX_OPTION_VALUES)
                    .toList();
            if (!split.isEmpty()) {
                options.put(key.substring(OPTION_PREFIX.length()), split);
            }
        });
        return options;
    }
}
