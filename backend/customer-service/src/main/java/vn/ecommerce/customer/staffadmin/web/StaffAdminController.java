package vn.ecommerce.customer.staffadmin.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.customer.staffadmin.internal.StaffAdminService;
import vn.ecommerce.customer.staffadmin.internal.dto.StaffCreateCommand;
import vn.ecommerce.customer.staffadmin.internal.dto.StaffView;

/** Chỉ ADMIN được quản lý tài khoản nhân viên (STAFF chỉ được thao tác trên khách). */
@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: nhân viên")
class StaffAdminController {

    private final StaffAdminService staffAdminService;

    @GetMapping
    @Operation(summary = "Danh sách nhân viên", description = "Thành viên group `staff` trong Keycloak.")
    List<StaffView> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return staffAdminService.listStaff(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo nhân viên", description = "Mật khẩu tạm, buộc đổi lần đầu; 409 nếu email đã có.")
    StaffView create(@Valid @RequestBody StaffCreateCommand command) {
        return staffAdminService.createStaff(command);
    }

    @PostMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Vô hiệu hóa nhân viên", description = "Đăng xuất mọi phiên đang có.")
    void disable(@PathVariable String id) {
        staffAdminService.setStaffEnabled(id, false);
    }

    @PostMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Kích hoạt lại nhân viên")
    void enable(@PathVariable String id) {
        staffAdminService.setStaffEnabled(id, true);
    }
}
