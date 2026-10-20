package vn.ecommerce.customer.address.web;

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
import vn.ecommerce.customer.address.AddressView;
import vn.ecommerce.customer.address.internal.AddressService;
import vn.ecommerce.customer.address.internal.AddressUpsertCommand;

@RestController
@RequestMapping("/api/customers/me/addresses")
@RequiredArgsConstructor
@Tag(name = "Sổ địa chỉ")
class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Địa chỉ của tôi", description = "Mặc định xếp trước, rồi mới nhất trước.")
    List<AddressView> list() {
        return addressService.listMine();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm địa chỉ", description = "Địa chỉ đầu tiên tự thành mặc định.")
    AddressView create(@Valid @RequestBody AddressUpsertCommand command) {
        return addressService.create(command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa địa chỉ", description = "Bắt buộc gửi `version`; 409 nếu lệch.")
    AddressView update(@PathVariable UUID id, @Valid @RequestBody AddressUpsertCommand command) {
        return addressService.update(id, command);
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "Đặt làm địa chỉ mặc định")
    AddressView setDefault(@PathVariable UUID id) {
        return addressService.setDefault(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa địa chỉ", description = "Xóa mềm; đơn hàng cũ vẫn giữ snapshot.")
    void delete(@PathVariable UUID id) {
        addressService.delete(id);
    }
}
