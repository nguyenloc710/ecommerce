package vn.ecommerce.catalog.product.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.ecommerce.catalog.product.internal.admin.BrandAdminService;
import vn.ecommerce.catalog.product.internal.admin.CategoryAdminService;
import vn.ecommerce.catalog.product.internal.admin.ProductAdminService;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryAdminView;
import vn.ecommerce.common.security.KeycloakRolesConverter;
import vn.ecommerce.common.security.ResourceServerAutoConfiguration;
import vn.ecommerce.common.web.CommonWebAutoConfiguration;
import vn.ecommerce.common.web.error.ConflictException;

/** Phân quyền `/api/admin/**` và hợp đồng lỗi của các controller admin catalog. */
@WebMvcTest(controllers = {CategoryAdminController.class, BrandAdminController.class, ProductAdminController.class})
@ImportAutoConfiguration({CommonWebAutoConfiguration.class, ResourceServerAutoConfiguration.class})
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs")
class CatalogAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CategoryAdminService categoryAdminService;
    @MockitoBean BrandAdminService brandAdminService;
    @MockitoBean ProductAdminService productAdminService;

    /** jwt() không tự chạy converter của ứng dụng nên truyền converter thật để claim roles → ROLE_*. */
    private static JwtRequestPostProcessor as(String... roles) {
        return jwt().jwt(j -> j.subject("user-1").claim("roles", List.of(roles)))
                .authorities(new KeycloakRolesConverter());
    }

    @Test
    void anonymous_gets401WithErrorBody() throws Exception {
        mockMvc.perform(get("/api/admin/catalog/categories").header("Accept-Language", "vi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("common.unauthorized"))
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để tiếp tục"));
    }

    @Test
    void customer_gets403() throws Exception {
        mockMvc.perform(get("/api/admin/catalog/categories").with(as("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("common.access-denied"));
    }

    @Test
    void staff_canCreateCategory() throws Exception {
        when(categoryAdminService.create(any())).thenReturn(new CategoryAdminView(UUID.randomUUID(), null, "Nhẫn", "nhan",
                null, null, 0, "ACTIVE", null, "user-1", 0));

        mockMvc.perform(post("/api/admin/catalog/categories").with(as("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nhẫn\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("nhan"));
    }

    @Test
    void invalidBody_returnsFieldViolations() throws Exception {
        mockMvc.perform(post("/api/admin/catalog/products").with(as("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"status\":\"WRONG\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("common.validation-failed"))
                .andExpect(jsonPath("$.errors[?(@.field=='name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='status')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='categoryId')]").exists());
    }

    @Test
    void conflict_isMappedTo409WithArguments() throws Exception {
        when(categoryAdminService.create(any())).thenThrow(new ConflictException("category.slug-taken", "nhan"));

        mockMvc.perform(post("/api/admin/catalog/categories").with(as("STAFF"))
                        .header("Accept-Language", "vi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nhẫn\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("category.slug-taken"))
                .andExpect(jsonPath("$.message").value("Slug \"nhan\" đã được dùng cho danh mục khác"));
    }

    @Test
    void staff_deleteBrand_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/catalog/brands/{id}", UUID.randomUUID()).with(as("STAFF")))
                .andExpect(status().isNoContent());
    }
}
