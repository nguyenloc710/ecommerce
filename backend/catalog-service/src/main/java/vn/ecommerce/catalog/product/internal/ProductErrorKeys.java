package vn.ecommerce.catalog.product.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Message key của module product; nội dung trong {@code messages*.properties}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductErrorKeys {

    /** Không có tham số. */
    public static final String PRODUCT_NOT_FOUND = "product.not-found";
    /** {0} = slug danh mục. */
    public static final String CATEGORY_NOT_FOUND = "category.not-found";
    /** {0} = slug thương hiệu. */
    public static final String BRAND_NOT_FOUND = "brand.not-found";
}
