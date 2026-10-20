package vn.ecommerce.catalog.inventory.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import vn.ecommerce.catalog.inventory.internal.dto.AvailabilityView;
import vn.ecommerce.catalog.inventory.internal.dto.SkuInfo;

/** Ghép thông tin SKU từ module product với tồn kho. */
public interface SkuLookupService {

    List<SkuInfo> lookup(Collection<UUID> skuIds);

    List<AvailabilityView> availability(Collection<UUID> skuIds);
}
