package vn.ecommerce.order;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Chặn module gọi chéo trái phép; sinh tài liệu module vào {@code target/spring-modulith-docs}. */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(OrderServiceApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
