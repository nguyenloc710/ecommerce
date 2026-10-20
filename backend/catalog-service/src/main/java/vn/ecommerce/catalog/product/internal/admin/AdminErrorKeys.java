package vn.ecommerce.catalog.product.internal.admin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Message key của phần quản trị catalog. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AdminErrorKeys {

    /** {0} = slug. */
    public static final String CATEGORY_SLUG_TAKEN = "category.slug-taken";
    public static final String CATEGORY_NOT_FOUND_ID = "category.not-found-id";
    public static final String CATEGORY_PARENT_NOT_FOUND = "category.parent-not-found";
    public static final String CATEGORY_PARENT_CYCLE = "category.parent-cycle";
    public static final String CATEGORY_HAS_CHILDREN = "category.has-children";
    /** {0} = số sản phẩm. */
    public static final String CATEGORY_HAS_PRODUCTS = "category.has-products";

    /** {0} = slug. */
    public static final String BRAND_SLUG_TAKEN = "brand.slug-taken";
    public static final String BRAND_NOT_FOUND_ID = "brand.not-found-id";
    /** {0} = số sản phẩm. */
    public static final String BRAND_IN_USE = "brand.in-use";

    /** {0} = slug. */
    public static final String PRODUCT_SLUG_TAKEN = "product.slug-taken";
    public static final String PRODUCT_NOT_FOUND_ID = "product.not-found-id";
    /** {0} = sku. */
    public static final String VARIANT_SKU_TAKEN = "variant.sku-taken";
    public static final String VARIANT_NOT_FOUND = "variant.not-found";
    public static final String IMAGE_VARIANT_MISMATCH = "image.variant-mismatch";
}
