package vn.ecommerce.catalog.product.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình module product.
 *
 * @param newLabelFor sản phẩm đăng trong khoảng này được gắn nhãn "Mới" trên thẻ
 * @param colorOption tên option trong {@code option_values} dùng làm chấm màu trên thẻ
 */
@ConfigurationProperties(prefix = "ecommerce.product")
public record ProductProperties(Duration newLabelFor, String colorOption) {

    public ProductProperties {
        newLabelFor = newLabelFor == null ? Duration.ofDays(30) : newLabelFor;
        colorOption = colorOption == null || colorOption.isBlank() ? "color" : colorOption;
    }
}
