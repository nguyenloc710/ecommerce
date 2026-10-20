package vn.ecommerce.customer.wishlist.web;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ecommerce.customer.wishlist.WishlistItemView;
import vn.ecommerce.customer.wishlist.internal.WishlistAddCommand;
import vn.ecommerce.customer.wishlist.internal.WishlistService;

@RestController
@RequestMapping("/api/customers/me/wishlist")
@RequiredArgsConstructor
@Tag(name = "Yêu thích")
class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Danh sách yêu thích của tôi")
    List<WishlistItemView> list() {
        return wishlistService.listMine();
    }

    @PutMapping("/{skuId}")
    @Operation(summary = "Thêm SKU vào yêu thích", description = "Idempotent: gọi lại trả mục đã có.")
    WishlistItemView add(@PathVariable UUID skuId, @Valid @RequestBody WishlistAddCommand command) {
        return wishlistService.add(skuId, command);
    }

    @DeleteMapping("/{skuId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bỏ yêu thích")
    void remove(@PathVariable UUID skuId) {
        wishlistService.remove(skuId);
    }
}
