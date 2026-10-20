package vn.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * Giỏ hàng, khuyến mãi, checkout, đơn hàng, thanh toán, vận chuyển.
 * <p>
 * Mỗi package con trực tiếp là một module Spring Modulith; module chỉ gọi nhau qua API public
 * (type ở package gốc của module) hoặc qua event. Kiểm tra bằng {@code ModularityTests}.
 */
@SpringBootApplication
@EntityScan(basePackages = {"vn.ecommerce.order", "vn.ecommerce.common.jpa"})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
