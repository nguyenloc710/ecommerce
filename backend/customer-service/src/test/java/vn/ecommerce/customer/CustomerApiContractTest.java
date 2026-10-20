package vn.ecommerce.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import vn.ecommerce.common.security.KeycloakRolesConverter;
import vn.ecommerce.common.security.ResourceServerAutoConfiguration;
import vn.ecommerce.common.web.CommonWebAutoConfiguration;
import vn.ecommerce.customer.address.DivisionView;
import vn.ecommerce.customer.address.internal.AddressService;
import vn.ecommerce.customer.address.internal.division.DivisionService;
import vn.ecommerce.customer.profile.CustomerProfileView;
import vn.ecommerce.customer.profile.internal.ProfileService;
import vn.ecommerce.customer.staffadmin.internal.StaffAdminService;
import vn.ecommerce.customer.wishlist.internal.WishlistService;

/**
 * Hợp đồng HTTP + bảo mật của customer-service: endpoint khách cần JWT, admin cần STAFF/ADMIN, staff cần ADMIN,
 * divisions công khai, internal cần INTERNAL; validate trả {@code ErrorResponse}.
 */
// Không liệt kê controller (package-private) → nạp mọi @RestController của service
@WebMvcTest
@ImportAutoConfiguration({CommonWebAutoConfiguration.class, ResourceServerAutoConfiguration.class})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs",
        "ecommerce.security.public-get-paths=/api/divisions/**"
})
class CustomerApiContractTest {

    private static final String SUB = "33333333-3333-3333-3333-333333333333";

    @Autowired MockMvc mockMvc;
    @MockitoBean ProfileService profileService;
    @MockitoBean AddressService addressService;
    @MockitoBean DivisionService divisionService;
    @MockitoBean WishlistService wishlistService;
    @MockitoBean StaffAdminService staffAdminService;

    private static JwtRequestPostProcessor as(String... roles) {
        return jwt().jwt(j -> j.subject(SUB).claim("email", "lan@example.com").claim("roles", List.of(roles)))
                .authorities(new KeycloakRolesConverter());
    }

    @Test
    void me_withoutToken_is401_withJsonBody() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("common.unauthorized"));
    }

    @Test
    void me_withCustomerToken_returnsProfile() throws Exception {
        when(profileService.ensureCurrentProfile()).thenReturn(
                new CustomerProfileView(UUID.fromString(SUB), "lan@example.com", "Lan", null, null, null, "ACTIVE", Instant.now(), 0));

        mockMvc.perform(get("/api/customers/me").with(as("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lan@example.com"));
    }

    @Test
    void updateMe_invalidPhone_is400_withFieldViolation() throws Exception {
        mockMvc.perform(put("/api/customers/me").with(as("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Lan\",\"phone\":\"12345\",\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("common.validation-failed"))
                .andExpect(jsonPath("$.errors[0].field").value("phone"));
        verify(profileService, never()).updateCurrent(any());
    }

    @Test
    void provinces_isPublic() throws Exception {
        when(divisionService.provinces()).thenReturn(List.of(new DivisionView("01", "Thành phố Hà Nội", "thành phố", null)));

        mockMvc.perform(get("/api/divisions/provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("01"));
    }

    @Test
    void adminCustomers_withCustomerRole_is403() throws Exception {
        mockMvc.perform(get("/api/admin/customers").with(as("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("common.access-denied"));
    }

    @Test
    void lockCustomer_withStaffRole_isAllowed() throws Exception {
        UUID target = UUID.randomUUID();

        mockMvc.perform(post("/api/admin/customers/{id}/lock", target).with(as("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"spam\"}"))
                .andExpect(status().isNoContent());
        verify(staffAdminService).lockCustomer(eq(target), any());
    }

    @Test
    void staffList_withStaffRole_is403_onlyAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/staff").with(as("STAFF")))
                .andExpect(status().isForbidden());
        verify(staffAdminService, never()).listStaff(0, 20);
    }

    @Test
    void staffList_withAdminRole_isOk() throws Exception {
        when(staffAdminService.listStaff(0, 20)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/staff").with(as("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void createStaff_shortPassword_is400() throws Exception {
        mockMvc.perform(post("/api/admin/staff").with(as("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@shop.vn\",\"fullName\":\"A\",\"temporaryPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("temporaryPassword"));
    }

    @Test
    void internalAddress_withCustomerRole_is403_withInternalRole_isOk() throws Exception {
        UUID customer = UUID.randomUUID();
        UUID address = UUID.randomUUID();
        mockMvc.perform(get("/internal/customers/{c}/addresses/{a}", customer, address).with(as("CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/internal/customers/{c}/addresses/{a}", customer, address).with(as("INTERNAL")))
                .andExpect(status().isOk());
        verify(addressService).getForCustomer(customer, address);
    }

    @Test
    void createAddress_withoutIsDefault_isAccepted() throws Exception {
        mockMvc.perform(post("/api/customers/me/addresses").with(as("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientName\":\"Lan\",\"phone\":\"0912345678\",\"provinceCode\":\"01\",\"wardCode\":\"00004\",\"line1\":\"1 Hàng Bài\"}"))
                .andExpect(status().isCreated());
        verify(addressService).create(any());
    }

    @Test
    void addWishlist_blankSlug_is400() throws Exception {
        mockMvc.perform(put("/api/customers/me/wishlist/{sku}", UUID.randomUUID()).with(as("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productSlug\":\" \"}"))
                .andExpect(status().isBadRequest());
        verify(wishlistService, never()).add(any(), any());
    }
}
