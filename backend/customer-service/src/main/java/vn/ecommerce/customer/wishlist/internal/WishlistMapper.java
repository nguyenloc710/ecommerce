package vn.ecommerce.customer.wishlist.internal;

import org.mapstruct.Mapper;
import vn.ecommerce.customer.wishlist.WishlistItemView;

@Mapper
public interface WishlistMapper {

    WishlistItemView toView(WishlistItem item);
}
