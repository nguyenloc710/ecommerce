package vn.ecommerce.order.cart.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.order.cart.CartView;
import vn.ecommerce.order.cart.internal.CartLineCommand;
import vn.ecommerce.order.cart.internal.CartMergeCommand;
import vn.ecommerce.order.cart.internal.CartService;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Giỏ hàng")
class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Giỏ của tôi", description = "Giá và tồn kho tra lại từ catalog mỗi lần gọi; `issues` liệt kê dòng có vấn đề.")
    CartView get() {
        return cartService.getMine();
    }

    @PutMapping("/items")
    @Operation(summary = "Đặt số lượng một dòng", description = "`quantity = 0` xóa dòng; dòng mới phải là SKU có trong catalog.")
    CartView setLine(@Valid @RequestBody CartLineCommand command) {
        return cartService.setLine(command);
    }

    @DeleteMapping("/items/{skuId}")
    @Operation(summary = "Xóa một dòng", description = "Kèm `engraving` nếu dòng có khắc tên.")
    CartView removeLine(@PathVariable UUID skuId, @RequestParam(required = false) String engraving) {
        return cartService.removeLine(skuId, engraving);
    }

    @PostMapping("/merge")
    @Operation(summary = "Gộp giỏ khách vãng lai sau khi đăng nhập", description = "Dòng trùng được cộng số lượng.")
    CartView merge(@Valid @RequestBody CartMergeCommand command) {
        return cartService.merge(command);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa sạch giỏ")
    void clear() {
        cartService.clearMine();
    }
}
