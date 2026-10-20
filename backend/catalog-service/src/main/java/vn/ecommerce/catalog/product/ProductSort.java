package vn.ecommerce.catalog.product;

import java.util.Locale;
import java.util.Optional;

public enum ProductSort {
    NEWEST,
    PRICE_ASC,
    PRICE_DESC;

    /** {@code newest | price_asc | price_desc}; giá trị lạ trả rỗng để controller báo lỗi validate. */
    public static Optional<ProductSort> fromParam(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(NEWEST);
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
