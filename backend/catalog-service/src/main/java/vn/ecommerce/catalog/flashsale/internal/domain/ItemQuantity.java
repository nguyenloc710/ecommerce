package vn.ecommerce.catalog.flashsale.internal.domain;

import java.util.UUID;

/** Tổng số lượng theo item (projection của các câu {@code group by}). */
public record ItemQuantity(UUID itemId, long quantity) {
}
