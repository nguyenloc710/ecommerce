package vn.ecommerce.catalog.inventory.internal.dto;

import java.util.UUID;

/** Một dòng bảng tồn kho admin (tồn kho + thông tin SKU tra từ module product). */
public record InventoryRow(UUID skuId, String skuCode, String productName, String variantLabel,
                           int onHand, int reserved, int available, int lowStockThreshold, boolean lowStock, long version) {
}
