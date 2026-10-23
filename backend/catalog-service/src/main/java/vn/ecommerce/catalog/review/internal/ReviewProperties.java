package vn.ecommerce.catalog.review.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param maxImages      số ảnh tối đa mỗi đánh giá
 * @param defaultPageSize số đánh giá mỗi trang ở trang sản phẩm
 */
@ConfigurationProperties(prefix = "ecommerce.review")
public record ReviewProperties(int maxImages, int defaultPageSize) {
}
