package vn.ecommerce.catalog.product.internal.domain;

/** FIXED: giá cố định trên variant; GOLD_RATE: giá = trọng lượng × giá vàng ngày + phí gia công (B-09, làm ở E7.3). */
public enum PricingType {
    FIXED,
    GOLD_RATE
}
