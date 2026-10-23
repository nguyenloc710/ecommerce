package vn.ecommerce.catalog.inventory.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import vn.ecommerce.catalog.inventory.internal.ReservationService;
import vn.ecommerce.catalog.inventory.internal.SkuLookupService;
import vn.ecommerce.catalog.inventory.internal.StockAdminService;
import vn.ecommerce.catalog.inventory.internal.dto.AvailabilityView;
import vn.ecommerce.catalog.inventory.internal.dto.ReservationResult;
import vn.ecommerce.common.security.KeycloakRolesConverter;
import vn.ecommerce.common.security.ResourceServerAutoConfiguration;
import vn.ecommerce.common.web.CommonWebAutoConfiguration;

@WebMvcTest(controllers = {AvailabilityController.class, InternalSkuController.class, InternalReservationController.class, InventoryAdminController.class})
@ImportAutoConfiguration({CommonWebAutoConfiguration.class, ResourceServerAutoConfiguration.class})
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs")
class InventoryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SkuLookupService skuLookupService;
    @MockitoBean ReservationService reservationService;
    @MockitoBean StockAdminService stockAdminService;

    private static JwtRequestPostProcessor as(String... roles) {
        return jwt().jwt(j -> j.subject("svc").claim("roles", List.of(roles))).authorities(new KeycloakRolesConverter());
    }

    @Test
    void availability_isPublic() throws Exception {
        UUID sku = UUID.randomUUID();
        when(skuLookupService.availability(anyCollection())).thenReturn(List.of(new AvailabilityView(sku, true, true, 2)));

        mockMvc.perform(get("/api/inventory/availability").param("skuIds", sku.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lowStock").value(true))
                .andExpect(jsonPath("$[0].remaining").value(2));
    }

    @Test
    void internalLookup_requiresInternalRole() throws Exception {
        String body = "{\"skuIds\":[\"" + UUID.randomUUID() + "\"]}";
        mockMvc.perform(post("/internal/skus/lookup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/skus/lookup").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/skus/lookup").with(as("INTERNAL")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void reserve_validatesBody_andReturnsResult() throws Exception {
        UUID order = UUID.randomUUID();
        UUID sku = UUID.randomUUID();
        when(reservationService.reserve(any(), any(), any())).thenReturn(new ReservationResult(order, "RESERVED", null, List.of()));

        mockMvc.perform(post("/internal/reservations").with(as("INTERNAL")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"" + order + "\",\"lines\":[{\"skuId\":\"" + sku + "\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("lines[0].quantity"));

        mockMvc.perform(post("/internal/reservations").with(as("INTERNAL")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"" + order + "\",\"lines\":[{\"skuId\":\"" + sku + "\",\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void adminUpdate_requiresStaff_andValidates() throws Exception {
        UUID sku = UUID.randomUUID();
        mockMvc.perform(put("/api/admin/inventory/{sku}", sku).with(as("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content("{\"onHand\":5}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/inventory/{sku}", sku).with(as("STAFF")).contentType(MediaType.APPLICATION_JSON).content("{\"onHand\":-1}"))
                .andExpect(status().isBadRequest());
    }
}
