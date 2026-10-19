package vn.ecommerce.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hồ sơ khách hàng, sổ địa chỉ, wishlist, quản lý nhân viên qua Keycloak Admin API.
 * <p>
 * Mỗi package con trực tiếp là một module Spring Modulith; module chỉ gọi nhau qua API public
 * (type ở package gốc của module) hoặc qua event. Kiểm tra bằng {@code ModularityTests}.
 */
@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
