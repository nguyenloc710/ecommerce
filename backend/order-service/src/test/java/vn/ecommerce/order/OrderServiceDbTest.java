package vn.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.security.HmacSignatures;
import vn.ecommerce.order.cart.internal.CartLineCommand;
import vn.ecommerce.order.cart.internal.CartService;
import vn.ecommerce.order.catalogclient.CatalogClient;
import vn.ecommerce.order.catalogclient.ReservationOutcome;
import vn.ecommerce.order.catalogclient.ReserveLine;
import vn.ecommerce.order.catalogclient.SkuSnapshot;
import vn.ecommerce.order.checkout.CheckoutResult;
import vn.ecommerce.order.checkout.internal.CheckoutRequest;
import vn.ecommerce.order.checkout.internal.CheckoutService;
import vn.ecommerce.order.customerclient.CustomerClient;
import vn.ecommerce.order.orders.OrderStatus;
import vn.ecommerce.order.orders.OrderView;
import vn.ecommerce.order.orders.internal.OrderAdminService;
import vn.ecommerce.order.orders.internal.OrderService;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.payment.internal.PaymentService;
import vn.ecommerce.order.promotion.VoucherView;
import vn.ecommerce.order.promotion.internal.PromotionService;
import vn.ecommerce.order.promotion.internal.VoucherUpsertCommand;
import vn.ecommerce.order.shipping.internal.ShippingService;

/**
 * Cả ứng dụng trên PostgreSQL thật, catalog/customer được mock: đặt COD (voucher, phí ship, outbox), idempotency,
 * đặt online + webhook thanh toán, admin tạo vận đơn + webhook giao hàng → DELIVERED, khách hủy đơn.
 */
/*
 * @DirtiesContext: đóng context ngay sau class, khi container PostgreSQL còn sống. Không có nó, context nằm trong cache
 * tới lúc JVM thoát, Spring Modulith truy vấn event chưa xong vào DB đã bị dừng và Hikari chờ đủ 30s (E0.7).
 * Callback afterAll chạy ngược thứ tự đăng ký nên Spring đóng trước, Testcontainers dừng container sau.
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.connection-timeout=2s"
})
class OrderServiceDbTest {

    private static final UUID CUSTOMER = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ADMIN = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID RING = UUID.fromString("40000000-0000-0000-0000-000000000702");
    private static final UUID NECKLACE = UUID.fromString("40000000-0000-0000-0000-000000000801");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean CatalogClient catalog;
    @MockitoBean CustomerClient customers;

    @Autowired CheckoutService checkout;
    @Autowired CartService cartService;
    @Autowired OrderService orderService;
    @Autowired OrderAdminService adminService;
    @Autowired PaymentService paymentService;
    @Autowired ShippingService shippingService;
    @Autowired PromotionService promotionService;
    @Autowired JdbcTemplate jdbc;
    @Value("${ecommerce.payment.mock.secret}") String paymentSecret;
    @Value("${ecommerce.shipping.mock.secret}") String shippingSecret;

    @BeforeEach
    void stubCatalog() {
        when(catalog.lookup(anyList())).thenReturn(List.of(
                new SkuSnapshot(RING, UUID.randomUUID(), "nhan-moissanite", "Nhẫn moissanite", "Bạc / 6", "RING-6", new BigDecimal("450000"), null, "/img/r.jpg", 5, true),
                new SkuSnapshot(NECKLACE, UUID.randomUUID(), "day-chuyen", "Dây chuyền", "Bạc", "NECK", new BigDecimal("300000"), null, "/img/n.jpg", 3, true)));
        when(catalog.reserve(any(), anyList())).thenAnswer(inv -> new ReservationOutcome(inv.getArgument(0), "RESERVED", Instant.now().plus(Duration.ofMinutes(15))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void codOrder_withVoucher_isConfirmed_andIdempotent() {
        signIn(ADMIN, "ADMIN");
        VoucherView voucher = promotionService.create(new VoucherUpsertCommand("SALE10", "PERCENT", BigDecimal.TEN, new BigDecimal("40000"),
                BigDecimal.ZERO, Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1)), 10, 1, "ACTIVE", "Giảm 10%", null));

        signIn(CUSTOMER, "CUSTOMER");
        cartService.setLine(new CartLineCommand(RING, "nhan-moissanite", 2, null));
        CheckoutRequest request = request(List.of(new CheckoutRequest.Line(RING, 1, "LAN"), new CheckoutRequest.Line(NECKLACE, 1, null)), "COD", "SALE10");

        CheckoutResult result = checkout.placeOrder(request, "idem-cod");
        OrderView order = result.order();

        assertThat(result.replayed()).isFalse();
        assertThat(order.code()).matches("ORD-\\d{6}-\\d{4}");
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED.name());
        assertThat(order.subtotal()).isEqualByComparingTo("750000");
        assertThat(order.discount()).as("10% = 75k nhưng trần 40k").isEqualByComparingTo("40000");
        assertThat(order.shippingFee()).as("tạm tính 750k ≥ 500k → freeship").isEqualByComparingTo("0");
        assertThat(order.total()).isEqualByComparingTo("710000");
        assertThat(order.history()).extracting(OrderView.History::to).containsExactly("PENDING", "CONFIRMED");
        assertThat(order.items()).hasSize(2).first().satisfies(i -> assertThat(i.engraving()).isEqualTo("LAN"));
        verify(catalog).reserve(order.id(), List.of(new ReserveLine(RING, 1), new ReserveLine(NECKLACE, 1)));
        assertThat(jdbc.queryForObject("select count(*) from event_publication", Long.class)).as("order.confirmed vào outbox").isPositive();
        assertThat(jdbc.queryForObject("select status from payment where order_id = ?", String.class, order.id())).isEqualTo("INIT");
        assertThat(promotionService.get(voucher.id()).usedCount()).isEqualTo(1);
        assertThat(cartService.getMine().lines()).as("dòng đã đặt bị dọn khỏi giỏ").isEmpty();

        CheckoutResult replay = checkout.placeOrder(request, "idem-cod");
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.order().id()).isEqualTo(order.id());
        assertThat(jdbc.queryForObject("select count(*) from orders", Long.class)).isEqualTo(1L);

        // Khách hủy khi chưa đóng gói; hủy lần hai bị từ chối
        OrderView cancelled = orderService.cancelMine(order.code(), "Đổi ý");
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancelReason()).isEqualTo("Đổi ý");
        assertThatThrownBy(() -> orderService.cancelMine(order.code(), null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void onlineOrder_webhookConfirms_thenAdminShips_andCarrierWebhookDelivers() {
        signIn(CUSTOMER, "CUSTOMER");
        CheckoutResult result = checkout.placeOrder(request(List.of(new CheckoutRequest.Line(NECKLACE, 1, null)), "MOCK_ONLINE", null), "idem-online");
        OrderView order = result.order();
        assertThat(order.status()).isEqualTo("AWAITING_PAYMENT");
        assertThat(order.shippingFee()).as("300k < 500k → phí GHN").isEqualByComparingTo("25000");
        assertThat(result.payUrl()).contains(order.id().toString());

        String body = "{\"orderId\":\"" + order.id() + "\",\"txnId\":\"MOCK-TXN-1\",\"status\":\"SUCCESS\",\"amount\":" + order.total().toPlainString() + "}";
        paymentService.handleWebhook(PaymentMethod.MOCK_ONLINE, HmacSignatures.sign(paymentSecret, body), body);
        paymentService.handleWebhook(PaymentMethod.MOCK_ONLINE, HmacSignatures.sign(paymentSecret, body), body); // gửi lại: idempotent
        OrderView paid = orderService.getMine(order.code());
        assertThat(paid.status()).isEqualTo("CONFIRMED");
        assertThat(paid.paidAt()).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from payment where provider_txn_id = 'MOCK-TXN-1'", Long.class)).isEqualTo(1L);

        signIn(ADMIN, "ADMIN");
        OrderView shipped = adminService.ship(order.code(), null);
        assertThat(shipped.status()).isEqualTo("SHIPPING");
        assertThat(shipped.shipment().trackingCode()).startsWith("MOCK-GHN-");
        assertThat(shipped.history()).extracting(OrderView.History::to).containsExactly("PENDING", "AWAITING_PAYMENT", "CONFIRMED", "PACKING", "SHIPPING");
        assertThat(jdbc.queryForObject("select count(*) from admin_audit_log where action = 'ORDER_SHIP'", Long.class)).isEqualTo(1L);

        String carrierBody = "{\"trackingCode\":\"" + shipped.shipment().trackingCode() + "\",\"status\":\"DELIVERED\"}";
        shippingService.handleMockWebhook(HmacSignatures.sign(shippingSecret, carrierBody), carrierBody);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(adminService.get(order.code()).status()).isEqualTo("DELIVERED"));

        assertThat(adminService.changeStatus(order.code(), OrderStatus.COMPLETED, null).status()).isEqualTo("COMPLETED");
        assertThatThrownBy(() -> adminService.cancel(order.code(), "muộn rồi")).isInstanceOf(ConflictException.class);
    }

    private static CheckoutRequest request(List<CheckoutRequest.Line> lines, String paymentMethod, String voucher) {
        CheckoutRequest.AddressInput address = new CheckoutRequest.AddressInput("Lan Nguyễn", "0912345678", "01", "Thành phố Hà Nội", "00004",
                "Phường Ba Đình", "1 Hàng Bài", null);
        return new CheckoutRequest(lines, null, address, paymentMethod, "GHN", voucher, "lan@example.com", null);
    }

    private static void signIn(UUID sub, String role) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(sub.toString()).claim("email", sub + "@example.com")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
