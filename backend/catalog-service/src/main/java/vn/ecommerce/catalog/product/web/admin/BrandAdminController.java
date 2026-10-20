package vn.ecommerce.catalog.product.web.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import vn.ecommerce.catalog.product.internal.admin.BrandAdminService;
import vn.ecommerce.catalog.product.internal.admin.dto.BrandAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.BrandUpsertCommand;

@RestController
@RequestMapping("/api/admin/catalog/brands")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin · Thương hiệu")
class BrandAdminController {

    private final BrandAdminService service;

    @GetMapping
    @Operation(summary = "Danh sách thương hiệu")
    PageView<BrandAdminView> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo thương hiệu", description = "409 `brand.slug-taken` nếu trùng slug.")
    BrandAdminView create(@Valid @RequestBody BrandUpsertCommand command) {
        return service.create(command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa thương hiệu", description = "Bắt buộc gửi `version`.")
    BrandAdminView update(@PathVariable UUID id, @Valid @RequestBody BrandUpsertCommand command) {
        return service.update(id, command);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa mềm thương hiệu", description = "409 `brand.in-use` nếu còn sản phẩm.")
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
