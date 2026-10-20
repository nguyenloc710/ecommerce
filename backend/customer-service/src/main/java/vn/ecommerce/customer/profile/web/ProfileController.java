package vn.ecommerce.customer.profile.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.customer.profile.CustomerProfileView;
import vn.ecommerce.customer.profile.internal.ProfileService;
import vn.ecommerce.customer.profile.internal.ProfileUpdateCommand;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Hồ sơ khách hàng")
class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/api/customers/me")
    @Operation(summary = "Hồ sơ của tôi", description = "Lần đầu gọi sau khi đăng nhập sẽ tự tạo hồ sơ từ JWT.")
    CustomerProfileView me() {
        return profileService.ensureCurrentProfile();
    }

    @PutMapping("/api/customers/me")
    @Operation(summary = "Cập nhật hồ sơ của tôi", description = "Bắt buộc gửi `version`; 409 nếu lệch.")
    CustomerProfileView update(@Valid @RequestBody ProfileUpdateCommand command) {
        return profileService.updateCurrent(command);
    }

    @GetMapping("/api/admin/customers")
    @Operation(summary = "Admin: danh sách khách hàng", description = "`q` tìm theo email, tên, số điện thoại.")
    PageResponse<CustomerProfileView> search(@RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return profileService.searchForAdmin(q, page, size);
    }

    @GetMapping("/api/admin/customers/{id}")
    @Operation(summary = "Admin: chi tiết khách hàng")
    CustomerProfileView detail(@PathVariable UUID id) {
        return profileService.getForAdmin(id);
    }
}
