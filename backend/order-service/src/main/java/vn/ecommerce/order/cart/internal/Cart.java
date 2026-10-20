package vn.ecommerce.order.cart.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Một giỏ cho mỗi khách đã đăng nhập; dòng khác nhau theo (sku, nội dung khắc). */
@Getter
@Entity
@Table(name = "cart")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends AuditableEntity {

    @Column(name = "customer_id", nullable = false, updatable = false, unique = true)
    private UUID customerId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart(UUID customerId) {
        this.customerId = customerId;
    }

    public Optional<CartItem> find(UUID skuId, String engraving) {
        return items.stream().filter(i -> i.getSkuId().equals(skuId) && Objects.equals(i.getEngraving(), engraving)).findFirst();
    }

    public CartItem addLine(UUID skuId, String productSlug, int quantity, String engraving) {
        CartItem item = new CartItem(this, skuId, productSlug, quantity, engraving);
        items.add(item);
        return item;
    }

    public void removeLine(CartItem item) {
        items.remove(item);
    }

    public void removeSkus(java.util.Collection<UUID> skuIds) {
        items.removeIf(i -> skuIds.contains(i.getSkuId()));
    }

    public int totalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
