package vn.ecommerce.catalog.product.web.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.internal.admin.ProductAdminService;
import vn.ecommerce.catalog.product.internal.admin.dto.ImageItemCommand;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminQuery;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminRow;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductUpsertCommand;
import vn.ecommerce.catalog.product.internal.admin.dto.VariantUpsertCommand;

@RestController
@RequestMapping("/api/admin/catalog/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin · Sản phẩm")
class ProductAdminController {

    private final ProductAdminService service;

    @GetMapping
    @Operation(summary = "Bảng sản phẩm", description = "Lọc theo từ khóa (tên/slug), trạng thái, danh mục. Sắp theo cập nhật mới nhất.")
    PageView<ProductAdminRow> page(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) @Pattern(regexp = "DRAFT|ACTIVE|HIDDEN", message = "{validation.enum}") String status,
                                   @RequestParam(required = false) UUID categoryId,
                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.page(new ProductAdminQuery(q, status, categoryId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sản phẩm (mọi trạng thái) kèm variant và ảnh")
    ProductAdminView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo sản phẩm", description = "409 `product.slug-taken`; 400 `category.not-found-id` / `brand.not-found-id`. Trạng thái ACTIVE sẽ set published_at.")
    ProductAdminView create(@Valid @RequestBody ProductUpsertCommand command) {
        return service.create(command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa sản phẩm", description = "Bắt buộc gửi `version`; 409 `common.concurrent-modification`.")
    ProductAdminView update(@PathVariable UUID id, @Valid @RequestBody ProductUpsertCommand command) {
        return service.update(id, command);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa mềm sản phẩm và variant")
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm variant (SKU)", description = "409 `variant.sku-taken` nếu trùng mã SKU.")
    ProductAdminView addVariant(@PathVariable UUID id, @Valid @RequestBody VariantUpsertCommand command) {
        return service.addVariant(id, command);
    }

    @PutMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Sửa variant", description = "Bắt buộc gửi `version` của variant.")
    ProductAdminView updateVariant(@PathVariable UUID id, @PathVariable UUID variantId, @Valid @RequestBody VariantUpsertCommand command) {
        return service.updateVariant(id, variantId, command);
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Xóa mềm variant")
    ProductAdminView deleteVariant(@PathVariable UUID id, @PathVariable UUID variantId) {
        return service.deleteVariant(id, variantId);
    }

    @PutMapping("/{id}/images")
    @Operation(summary = "Thay toàn bộ danh sách ảnh theo thứ tự", description = "400 `image.variant-mismatch` nếu ảnh gắn variant không thuộc sản phẩm.")
    ProductAdminView replaceImages(@PathVariable UUID id, @RequestBody List<@Valid ImageItemCommand> images) {
        return service.replaceImages(id, images);
    }
}
