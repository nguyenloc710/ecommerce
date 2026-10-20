package vn.ecommerce.catalog.product.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.ecommerce.catalog.product.FacetsView;
import vn.ecommerce.catalog.product.ProductListQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.ProductQueryService;
import vn.ecommerce.common.security.ResourceServerAutoConfiguration;
import vn.ecommerce.common.web.CommonWebAutoConfiguration;
import vn.ecommerce.common.web.error.NotFoundException;

/** Hợp đồng HTTP: endpoint công khai không cần token, lỗi trả đúng {@code ErrorResponse}, validate tham số. */
@WebMvcTest(controllers = {ProductController.class, CategoryController.class})
@ImportAutoConfiguration({CommonWebAutoConfiguration.class, ResourceServerAutoConfiguration.class})
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs")
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProductQueryService productQueryService;

    @Test
    void list_isPublic_andReturnsPage() throws Exception {
        when(productQueryService.listProducts(any())).thenReturn(new PageView<>(List.of(), 0, 24, 0, false));

        mockMvc.perform(get("/api/products").param("category", "nhan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(24))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void list_parsesOptionFilters_fromOptPrefixedParams() throws Exception {
        when(productQueryService.listProducts(any())).thenReturn(new PageView<>(List.of(), 0, 24, 0, false));

        mockMvc.perform(get("/api/products").param("q", "nhan").param("opt.size", "6,7").param("opt.color", "Bạc").param("priceMax", "900000"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductListQuery> query = ArgumentCaptor.forClass(ProductListQuery.class);
        verify(productQueryService).listProducts(query.capture());
        assertThat(query.getValue().q()).isEqualTo("nhan");
        assertThat(query.getValue().options()).containsEntry("size", List.of("6", "7")).containsEntry("color", List.of("Bạc"));
        assertThat(query.getValue().priceMax()).isEqualByComparingTo("900000");
    }

    @Test
    void facets_isPublic() throws Exception {
        when(productQueryService.facets(any())).thenReturn(new FacetsView(List.of(), List.of(), null, null));

        mockMvc.perform(get("/api/products/facets").param("category", "nhan")).andExpect(status().isOk());
    }

    @Test
    void list_rejectsInvalidSort_withValidationError() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "random"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("common.validation-failed"))
                .andExpect(jsonPath("$.traceId").hasJsonPath());
    }

    @Test
    void list_rejectsPageSizeOverLimit() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detail_returns404WithMessageKey_whenMissing() throws Exception {
        when(productQueryService.productDetail("x")).thenThrow(new NotFoundException("product.not-found"));

        mockMvc.perform(get("/api/products/x").header("Accept-Language", "vi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("product.not-found"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm"));
    }

    @Test
    void categories_isPublic() throws Exception {
        when(productQueryService.categoryTree()).thenReturn(List.of());

        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
    }
}
