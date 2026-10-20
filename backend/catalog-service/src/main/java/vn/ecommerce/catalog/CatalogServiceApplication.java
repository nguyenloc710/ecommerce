package vn.ecommerce.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * Danh mục, sản phẩm, SKU, tồn kho, tìm kiếm, review, media.
 * <p>
 * Mỗi package con trực tiếp là một module Spring Modulith; module chỉ gọi nhau qua API public
 * (type ở package gốc của module) hoặc qua event. Kiểm tra bằng {@code ModularityTests}.
 * {@code @EntityScan} gồm cả {@code common.jpa} để Hibernate Envers thấy revision entity.
 */
@SpringBootApplication
@EntityScan(basePackages = {"vn.ecommerce.catalog", "vn.ecommerce.common.jpa"})
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
