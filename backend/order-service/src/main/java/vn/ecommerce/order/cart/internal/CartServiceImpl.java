package vn.ecommerce.order.cart.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.order.cart.CartView;
import vn.ecommerce.order.catalogclient.CatalogClient;
import vn.ecommerce.order.catalogclient.SkuSnapshot;

@Service
@RequiredArgsConstructor
@Transactional
class CartServiceImpl implements CartService {

    static final String ISSUE_NOT_SELLABLE = "inventory.sku-not-sellable";
    static final String ISSUE_INSUFFICIENT = "inventory.insufficient";
    static final String ISSUE_NOT_FOUND = "cart.sku-not-found";

    private final CartRepository repository;
    private final CatalogClient catalog;
    private final CartProperties properties;
    private final CurrentUser currentUser;
    private final MessageSource messages;

    @Override
    @Transactional(readOnly = true)
    public CartView getMine() {
        return repository.findByCustomerId(currentId()).map(this::toView).orElseGet(CartServiceImpl::emptyView);
    }

    @Override
    public CartView setLine(CartLineCommand command) {
        Cart cart = cartOf(currentId());
        String engraving = command.normalizedEngraving();
        CartItem existing = cart.find(command.skuId(), engraving).orElse(null);
        if (command.quantity() == 0) {
            if (existing != null) {
                cart.removeLine(existing);
            }
        } else if (existing != null) {
            existing.setQuantity(capQuantity(command.quantity()));
        } else {
            addValidatedLine(cart, command, command.quantity());
        }
        return toView(repository.save(cart));
    }

    @Override
    public CartView removeLine(UUID skuId, String engraving) {
        Cart cart = cartOf(currentId());
        cart.find(skuId, engraving == null || engraving.isBlank() ? null : engraving).ifPresent(cart::removeLine);
        return toView(repository.save(cart));
    }

    @Override
    public CartView merge(CartMergeCommand command) {
        Cart cart = cartOf(currentId());
        for (CartLineCommand line : command.lines()) {
            if (line.quantity() == 0) {
                continue;
            }
            String engraving = line.normalizedEngraving();
            CartItem existing = cart.find(line.skuId(), engraving).orElse(null);
            if (existing != null) {
                existing.setQuantity(capQuantity(existing.getQuantity() + line.quantity()));
            } else {
                addValidatedLine(cart, line, line.quantity());
            }
        }
        return toView(repository.save(cart));
    }

    @Override
    public void clearMine() {
        repository.findByCustomerId(currentId()).ifPresent(cart -> {
            cart.getItems().clear();
            repository.save(cart);
        });
    }

    @Override
    public void removeLines(UUID customerId, Collection<UUID> skuIds) {
        repository.findByCustomerId(customerId).ifPresent(cart -> {
            cart.removeSkus(skuIds);
            repository.save(cart);
        });
    }

    private void addValidatedLine(Cart cart, CartLineCommand line, int quantity) {
        if (cart.getItems().size() >= properties.maxLines()) {
            throw new BadRequestException(CartErrorKeys.TOO_MANY_LINES, properties.maxLines());
        }
        // Chỉ nhận SKU catalog biết; giá không lưu nên không cần tra lại ở đây
        if (catalog.lookup(List.of(line.skuId())).isEmpty()) {
            throw new BadRequestException(CartErrorKeys.SKU_NOT_FOUND);
        }
        cart.addLine(line.skuId(), line.productSlug().trim(), capQuantity(quantity), line.normalizedEngraving());
    }

    private int capQuantity(int quantity) {
        if (quantity > properties.maxQuantityPerLine()) {
            throw new BadRequestException(CartErrorKeys.QUANTITY_EXCEEDED, properties.maxQuantityPerLine());
        }
        return quantity;
    }

    private Cart cartOf(UUID customerId) {
        return repository.findByCustomerId(customerId).orElseGet(() -> repository.save(new Cart(customerId)));
    }

    private UUID currentId() {
        return UUID.fromString(currentUser.requireId());
    }

    /** Ghép giá/tồn kho hiện tại từ catalog; dòng có vấn đề vẫn hiển thị kèm cảnh báo để khách tự sửa. */
    private CartView toView(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return emptyView();
        }
        List<UUID> skuIds = cart.getItems().stream().map(CartItem::getSkuId).distinct().toList();
        Map<UUID, SkuSnapshot> skus = catalog.lookup(skuIds).stream().collect(Collectors.toMap(SkuSnapshot::skuId, Function.identity()));
        Locale locale = LocaleContextHolder.getLocale();
        List<CartView.Line> lines = new ArrayList<>();
        List<CartView.Issue> issues = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            SkuSnapshot sku = skus.get(item.getSkuId());
            if (sku == null) {
                issues.add(issue(item.getSkuId(), ISSUE_NOT_FOUND, locale));
                continue;
            }
            BigDecimal lineTotal = sku.price().multiply(BigDecimal.valueOf(item.getQuantity()));
            lines.add(new CartView.Line(sku.skuId(), sku.productSlug(), sku.productName(), sku.variantLabel(), sku.imageUrl(),
                    sku.price(), sku.compareAtPrice(), item.getQuantity(), item.getEngraving(), lineTotal, sku.available(), sku.sellable()));
            if (!sku.sellable()) {
                issues.add(issue(sku.skuId(), ISSUE_NOT_SELLABLE, locale));
            } else if (sku.available() < item.getQuantity()) {
                issues.add(issue(sku.skuId(), ISSUE_INSUFFICIENT, locale));
            } else {
                subtotal = subtotal.add(lineTotal);
            }
        }
        return new CartView(lines, subtotal, cart.totalQuantity(), issues);
    }

    private CartView.Issue issue(UUID skuId, String code, Locale locale) {
        return new CartView.Issue(skuId, code, messages.getMessage(code, null, code, locale));
    }

    private static CartView emptyView() {
        return new CartView(List.of(), BigDecimal.ZERO, 0, List.of());
    }
}
