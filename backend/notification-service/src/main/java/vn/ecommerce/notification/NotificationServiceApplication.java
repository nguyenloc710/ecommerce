package vn.ecommerce.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gửi email, SMS, Zalo ZNS theo event nghiệp vụ.
 * <p>
 * Mỗi package con trực tiếp là một module Spring Modulith; module chỉ gọi nhau qua API public
 * (type ở package gốc của module) hoặc qua event. Kiểm tra bằng {@code ModularityTests}.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
