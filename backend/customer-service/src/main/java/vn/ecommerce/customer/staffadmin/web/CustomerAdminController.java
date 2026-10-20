package vn.ecommerce.customer.staffadmin.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.customer.staffadmin.internal.StaffAdminService;
import vn.ecommerce.customer.staffadmin.internal.dto.LockCommand;

/** Nhân viên (STAFF/ADMIN) khóa hoặc mở tài khoản khách. */
@RestController
@RequestMapping("/api/admin/customers/{id}")
@RequiredArgsConstructor
@Tag(name = "Admin: tài khoản khách")
class CustomerAdminController {

    private final StaffAdminService staffAdminService;

    @PostMapping("/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Khóa khách hàng", description = "Chặn đăng nhập ở Keycloak, đăng xuất mọi phiên, ghi audit log.")
    void lock(@PathVariable UUID id, @Valid @RequestBody(required = false) LockCommand command) {
        staffAdminService.lockCustomer(id, command);
    }

    @PostMapping("/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mở khóa khách hàng")
    void unlock(@PathVariable UUID id) {
        staffAdminService.unlockCustomer(id);
    }
}
