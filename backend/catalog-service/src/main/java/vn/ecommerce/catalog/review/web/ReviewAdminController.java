package vn.ecommerce.catalog.review.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.catalog.review.internal.ReviewAdminService;
import vn.ecommerce.catalog.review.internal.ReviewDtos.AdminReviewRow;
import vn.ecommerce.catalog.review.internal.ReviewDtos.RejectCommand;
import vn.ecommerce.common.web.error.PageResponse;

@RestController
@RequestMapping("/api/admin/catalog/reviews")
@RequiredArgsConstructor
@Tag(name = "Quản trị đánh giá")
class ReviewAdminController {

    private final ReviewAdminService adminService;

    @GetMapping
    @Operation(summary = "Danh sách đánh giá theo trạng thái", description = "`status` = PENDING (mặc định, cũ nhất trước) | APPROVED | REJECTED.")
    PageResponse<AdminReviewRow> list(@RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return adminService.list(status, page, size);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Duyệt cho hiện trên trang sản phẩm")
    AdminReviewRow approve(@PathVariable UUID id) {
        return adminService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Từ chối hoặc ẩn đánh giá", description = "Bắt buộc lý do; ghi admin_audit_log.")
    AdminReviewRow reject(@PathVariable UUID id, @Valid @RequestBody RejectCommand command) {
        return adminService.reject(id, command.reason());
    }
}
