package vn.ecommerce.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.cart.internal.CartService;
import vn.ecommerce.order.checkout.internal.CheckoutService;
import vn.ecommerce.order.orders.internal.OrderAdminService;
import vn.ecommerce.order.orders.internal.OrderService;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.internal.PaymentService;
import vn.ecommerce.order.promotion.VoucherView;
import vn.ecommerce.order.promotion.internal.PromotionService;
import vn.ecommerce.order.shipping.internal.ShippingService;

/**
 * Hợp đồng HTTP + bảo mật của order-service: giỏ/đơn cần JWT, admin đơn cần STAFF, voucher admin cần ADMIN,
 * webhook không cần JWT, Idempotency-Key đi tới service, validate trả {@code ErrorResponse}.
 */
// Không liệt kê controller (package-private) → nạp mọi @RestController của service
@WebMvcTest
@ImportAutoConfiguration({CommonWebAutoConfiguration.class, ResourceServerAutoConfiguration.class})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs",
        "ecommerce.security.public-paths=/webhooks/**"
})
class OrderApiContractTest {

    private static final String SUB = "55555555-5555-5555-5555-555555555555";

    @Autowired MockMvc mockMvc;
    @MockitoBean CartService cartService;
    @MockitoBean PromotionService promotionService;
    @MockitoBean ShippingService shippingService;
    @MockitoBean CheckoutService checkoutService;
    @MockitoBean OrderService orderService;
    @MockitoBean OrderAdminService orderAdminService;
    @MockitoBean PaymentService paymentService;

    private static JwtRequestPostProcessor as(String... roles) {
        return jwt().jwt(j -> j.subject(SUB).claim("email", "lan@example.com").claim("roles", List.of(roles)))
                .authorities(new KeycloakRolesConverter());
    }

    @Test
    void cart_withoutToken_is401() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("common.unauthorized"));
    }

    @Test
    void paymentWebhook_isPublic_andReachesService() throws Exception {
        mockMvc.perform(post("/webhooks/payments/mock").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", "abc").content("{\"orderId\":\"x\"}"))
                .andExpect(status().isNoContent());
        verify(paymentService).handleWebhook(eq(PaymentMethod.MOCK_ONLINE), eq("abc"), eq("{\"orderId\":\"x\"}"));
    }

    @Test
    void paymentWebhook_unknownProvider_is404() throws Exception {
        mockMvc.perform(post("/webhooks/payments/vnpay").contentType(MediaType.APPLICATION_JSON).header("X-Signature", "abc").content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shippingWebhook_isPublic() throws Exception {
        mockMvc.perform(post("/webhooks/shipping/mock").contentType(MediaType.APPLICATION_JSON).header("X-Signature", "s").content("{}"))
                .andExpect(status().isNoContent());
        verify(shippingService).handleMockWebhook("s", "{}");
    }

    @Test
    void adminOrders_customer403_staff200() throws Exception {
        when(orderAdminService.search(any(), any(), eq(0), eq(20))).thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/admin/orders").with(as("CUSTOMER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders").param("status", "CONFIRMED").with(as("STAFF"))).andExpect(status().isOk());
    }

    @Test
    void adminVouchers_staff403_admin201() throws Exception {
        String body = "{\"code\":\"SALE10\",\"type\":\"PERCENT\",\"value\":10,\"startsAt\":\"2026-01-01T00:00:00Z\",\"endsAt\":\"2026-12-31T00:00:00Z\"}";
        when(promotionService.create(any())).thenReturn(new VoucherView(UUID.randomUUID(), "SALE10", "PERCENT", BigDecimal.TEN, null,
                BigDecimal.ZERO, Instant.now(), Instant.now(), null, 0, 1, "ACTIVE", null, 0));

        mockMvc.perform(post("/api/admin/vouchers").with(as("STAFF")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/vouchers").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SALE10"));
    }

    @Test
    void placeOrder_passesIdempotencyKeyToService() throws Exception {
        String body = "{\"lines\":[{\"skuId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}],\"paymentMethod\":\"COD\"}";

        mockMvc.perform(post("/api/orders").with(as("CUSTOMER")).header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is5xxServerError()); // mock trả null → không quan trọng, chỉ cần tham số tới service
        verify(checkoutService).placeOrder(any(), eq("k-1"));
    }

    @Test
    void placeOrder_emptyLines_is400_andServiceNotCalled() throws Exception {
        mockMvc.perform(post("/api/orders").with(as("CUSTOMER")).header("Idempotency-Key", "k-2")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lines\":[],\"paymentMethod\":\"COD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("common.validation-failed"))
                .andExpect(jsonPath("$.errors[0].field").value("lines"));
        verify(checkoutService, never()).placeOrder(any(), any());
    }

    @Test
    void quote_invalidInlineAddressPhone_is400() throws Exception {
        String body = "{\"lines\":[{\"skuId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}],"
                + "\"address\":{\"recipientName\":\"Lan\",\"phone\":\"123\",\"provinceCode\":\"01\",\"provinceName\":\"HN\",\"wardCode\":\"00004\",\"wardName\":\"BD\",\"line1\":\"1\"}}";

        mockMvc.perform(post("/api/checkout/quote").with(as("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("address.phone"));
    }

    @Test
    void myOrders_requiresToken_andWorksWithCustomer() throws Exception {
        when(orderService.listMine(0, 10)).thenReturn(PageResponse.of(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/orders/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders/me").with(as("CUSTOMER"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
}
