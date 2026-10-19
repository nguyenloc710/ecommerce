package vn.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** API Gateway: cửa vào duy nhất cho {@code /api/**} và {@code /webhooks/**} từ storefront và đối tác. */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
