package vn.ecommerce.catalog.inventory.internal.dto;

import java.util.UUID;

/** Tồn kho công khai cho PDP: không lộ số chính xác khi còn nhiều, chỉ báo "sắp hết" và số còn lại khi ít. */
public record AvailabilityView(UUID skuId, boolean inStock, boolean lowStock, Integer remaining) {
}
