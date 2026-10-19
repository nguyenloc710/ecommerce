package vn.ecommerce.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Danh mục, sản phẩm, SKU, tồn kho, tìm kiếm, review, media.
 * <p>
 * Mỗi package con trực tiếp là một module Spring Modulith; module chỉ gọi nhau qua API public
 * (type ở package gốc của module) hoặc qua event. Kiểm tra bằng {@code ModularityTests}.
 */
@SpringBootApplication
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
