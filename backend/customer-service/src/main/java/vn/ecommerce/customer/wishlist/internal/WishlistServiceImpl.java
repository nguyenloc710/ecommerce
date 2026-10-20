package vn.ecommerce.customer.wishlist.internal;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.customer.profile.ProfileApi;
import vn.ecommerce.customer.wishlist.WishlistItemView;

@Service
@RequiredArgsConstructor
@Transactional
class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository repository;
    private final WishlistMapper mapper;
    private final CurrentUser currentUser;
    private final ProfileApi profileApi;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemView> listMine() {
        return repository.findByCustomerIdOrderByCreatedAtDesc(currentId()).stream().map(mapper::toView).toList();
    }

    @Override
    public WishlistItemView add(UUID skuId, WishlistAddCommand command) {
        UUID customerId = profileApi.ensureCurrentProfile().id();
        WishlistItem item = repository.findByCustomerIdAndSkuId(customerId, skuId)
                .orElseGet(() -> repository.save(new WishlistItem(customerId, skuId, command.productSlug().trim())));
        return mapper.toView(item);
    }

    @Override
    public void remove(UUID skuId) {
        repository.deleteByCustomerIdAndSkuId(currentId(), skuId);
    }

    private UUID currentId() {
        return UUID.fromString(currentUser.requireId());
    }
}
