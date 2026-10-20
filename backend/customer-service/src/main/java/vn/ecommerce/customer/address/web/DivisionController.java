package vn.ecommerce.customer.address.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.customer.address.DivisionView;
import vn.ecommerce.customer.address.internal.division.DivisionService;

/** Công khai (không cần đăng nhập) để form checkout của khách vãng lai cũng dùng được. */
@RestController
@RequestMapping("/api/divisions")
@RequiredArgsConstructor
@Tag(name = "Đơn vị hành chính")
class DivisionController {

    private final DivisionService divisionService;

    @GetMapping("/provinces")
    @Operation(summary = "Danh sách tỉnh/thành (34 đơn vị)")
    List<DivisionView> provinces() {
        return divisionService.provinces();
    }

    @GetMapping("/provinces/{code}/wards")
    @Operation(summary = "Phường/xã của một tỉnh/thành")
    List<DivisionView> wards(@PathVariable String code) {
        return divisionService.wards(code);
    }
}
