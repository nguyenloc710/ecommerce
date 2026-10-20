package vn.ecommerce.catalog.product.web.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.product.internal.admin.CategoryAdminService;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryUpsertCommand;

/** Yêu cầu role STAFF/ADMIN (chain chung chặn {@code /api/admin/**}). */
@RestController
@RequestMapping("/api/admin/catalog/categories")
@RequiredArgsConstructor
@Tag(name = "Admin · Danh mục")
class CategoryAdminController {

    private final CategoryAdminService service;

    @GetMapping
    @Operation(summary = "Mọi danh mục (phẳng, có parentId), kể cả HIDDEN")
    List<CategoryAdminView> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo danh mục", description = "409 `category.slug-taken` nếu trùng slug; 400 `category.parent-not-found`.")
    CategoryAdminView create(@Valid @RequestBody CategoryUpsertCommand command) {
        return service.create(command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa danh mục", description = "Bắt buộc gửi `version`; 409 `common.concurrent-modification` nếu đã bị người khác sửa.")
    CategoryAdminView update(@PathVariable UUID id, @Valid @RequestBody CategoryUpsertCommand command) {
        return service.update(id, command);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa mềm danh mục", description = "409 `category.has-children` / `category.has-products` nếu còn phụ thuộc.")
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
