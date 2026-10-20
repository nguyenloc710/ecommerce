package vn.ecommerce.order.cart.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param maxLines           số dòng tối đa trong giỏ
 * @param maxQuantityPerLine số lượng tối đa mỗi dòng
 */
@ConfigurationProperties(prefix = "ecommerce.cart")
public record CartProperties(Integer maxLines, Integer maxQuantityPerLine) {

    public CartProperties {
        maxLines = maxLines == null ? 50 : maxLines;
        maxQuantityPerLine = maxQuantityPerLine == null ? 99 : maxQuantityPerLine;
    }
}
