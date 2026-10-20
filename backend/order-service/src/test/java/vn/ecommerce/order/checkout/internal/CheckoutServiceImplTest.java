package vn.ecommerce.order.checkout.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.order.cart.CartApi;
import vn.ecommerce.order.catalogclient.CatalogClient;
import vn.ecommerce.order.catalogclient.ReservationOutcome;
import vn.ecommerce.order.catalogclient.ReserveLine;
import vn.ecommerce.order.catalogclient.SkuSnapshot;
import vn.ecommerce.order.checkout.CheckoutQuoteView;
import vn.ecommerce.order.checkout.CheckoutResult;
import vn.ecommerce.order.customerclient.CustomerClient;
import vn.ecommerce.order.orders.OrderView;
import vn.ecommerce.order.orders.OrdersApi;
import vn.ecommerce.order.orders.PlaceOrderCommand;
import vn.ecommerce.order.orders.ShippingAddress;
import vn.ecommerce.order.payment.PaymentApi;
import vn.ecommerce.order.payment.PaymentInitResult;
import vn.ecommerce.order.payment.PaymentMethod;
import vn.ecommerce.order.promotion.DiscountQuote;
import vn.ecommerce.order.promotion.PromotionApi;
import vn.ecommerce.order.shipping.ShippingApi;
import vn.ecommerce.order.shipping.ShippingOption;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    private static final UUID CUSTOMER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID RING = UUID.fromString("40000000-0000-0000-0000-000000000702");
    private static final UUID NECKLACE = UUID.fromString("40000000-0000-0000-0000-000000000801");

    @Mock CatalogClient catalog;
    @Mock CustomerClient customers;
    @Mock PromotionApi promotions;
    @Mock ShippingApi shipping;
    @Mock OrdersApi orders;
    @Mock PaymentApi payments;
    @Mock CartApi cart;
    @Mock CurrentUser currentUser;
    @Mock TransactionTemplate transactions;

    private CheckoutServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new CheckoutServiceImpl(new PricingStep(catalog), new AddressResolver(customers), promotions, shipping, catalog, orders,
                payments, cart, currentUser, transactions);
        lenient().when(currentUser.requireId()).thenReturn(CUSTOMER.toString());
        lenient().when(currentUser.email()).thenReturn(Optional.of("lan@example.com"));
        lenient().when(transactions.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(mock(TransactionStatus.class)));
        lenient().when(catalog.lookup(anyList())).thenReturn(List.of(
                sku(RING, "Nhẫn moissanite", "450000", 5, true),
                sku(NECKLACE, "Dây chuyền", "300000", 1, true)));
        lenient().when(shipping.option(eq("GHN"), any(), any())).thenReturn(new ShippingOption("GHN", "Giao Hàng Nhanh", new BigDecimal("25000"), 3));
        lenient().when(shipping.options(any(), any())).thenReturn(List.of(new ShippingOption("GHN", "Giao Hàng Nhanh", new BigDecimal("25000"), 3)));
    }

    @Test
    void placeOrder_cod_reservesAggregatedLines_placesConfirmedOrder_andClearsCart() {
        when(orders.findByIdempotencyKey(CUSTOMER, "key-1")).thenReturn(Optional.empty());
        when(catalog.reserve(any(), anyList())).thenReturn(new ReservationOutcome(null, "RESERVED", Instant.now()));
        when(orders.place(any())).thenAnswer(inv -> orderView(inv.getArgument(0)));
        when(payments.initiate(any(), any(), any(), eq(PaymentMethod.COD))).thenReturn(new PaymentInitResult(UUID.randomUUID(), PaymentMethod.COD, "INIT", null));

        // Cùng nhẫn hai dòng (một khắc tên, một không) + dây chuyền
        CheckoutRequest request = request(List.of(line(RING, 1, "LAN"), line(RING, 1, null), line(NECKLACE, 1, null)), "COD", null);
        CheckoutResult result = service.placeOrder(request, "key-1");

        assertThat(result.replayed()).isFalse();
        assertThat(result.payUrl()).isNull();
        ArgumentCaptor<List<ReserveLine>> reserved = ArgumentCaptor.forClass(List.class);
        verify(catalog).reserve(any(), reserved.capture());
        assertThat(reserved.getValue()).containsExactly(new ReserveLine(RING, 2), new ReserveLine(NECKLACE, 1));

        ArgumentCaptor<PlaceOrderCommand> placed = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(orders).place(placed.capture());
        PlaceOrderCommand cmd = placed.getValue();
        assertThat(cmd.confirmImmediately()).isTrue();
        assertThat(cmd.lines()).hasSize(3);
        assertThat(cmd.subtotal()).isEqualByComparingTo("1200000");
        assertThat(cmd.shippingFee()).isEqualByComparingTo("25000");
        assertThat(cmd.contactEmail()).isEqualTo("lan@example.com");
        assertThat(cmd.address().provinceCode()).isEqualTo("01");
        verify(cart).removeLines(CUSTOMER, List.of(RING, NECKLACE));
        verify(catalog, never()).release(any());
    }

    @Test
    void placeOrder_online_returnsPayUrl_andAwaitsPayment() {
        when(orders.findByIdempotencyKey(CUSTOMER, "key-2")).thenReturn(Optional.empty());
        when(catalog.reserve(any(), anyList())).thenReturn(new ReservationOutcome(null, "RESERVED", Instant.now()));
        when(orders.place(any())).thenAnswer(inv -> orderView(inv.getArgument(0)));
        when(payments.initiate(any(), any(), any(), eq(PaymentMethod.MOCK_ONLINE)))
                .thenReturn(new PaymentInitResult(UUID.randomUUID(), PaymentMethod.MOCK_ONLINE, "PENDING", "http://pay/x"));

        CheckoutResult result = service.placeOrder(request(List.of(line(RING, 1, null)), "MOCK_ONLINE", null), "key-2");

        assertThat(result.payUrl()).isEqualTo("http://pay/x");
        ArgumentCaptor<PlaceOrderCommand> placed = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(orders).place(placed.capture());
        assertThat(placed.getValue().confirmImmediately()).isFalse();
    }

    @Test
    void placeOrder_sameIdempotencyKey_replaysExistingOrder_withoutTouchingCatalog() {
        OrderView existing = mock(OrderView.class);
        when(orders.findByIdempotencyKey(CUSTOMER, "key-1")).thenReturn(Optional.of(existing));

        CheckoutResult result = service.placeOrder(request(List.of(line(RING, 1, null)), "COD", null), "key-1");

        assertThat(result.replayed()).isTrue();
        assertThat(result.order()).isSameAs(existing);
        verifyNoInteractions(catalog, payments, cart);
    }

    @Test
    void placeOrder_withoutIdempotencyKey_isBadRequest() {
        assertThatThrownBy(() -> service.placeOrder(request(List.of(line(RING, 1, null)), "COD", null), " "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(CheckoutErrorKeys.IDEMPOTENCY_KEY_REQUIRED);
        verifyNoInteractions(catalog, orders);
    }

    @Test
    void placeOrder_insufficientStock_failsBeforeReserving() {
        when(orders.findByIdempotencyKey(any(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.placeOrder(request(List.of(line(NECKLACE, 2, null)), "COD", null), "key-3"))
                .isInstanceOf(ConflictException.class)
                .hasMessage(CheckoutErrorKeys.INSUFFICIENT_STOCK);
        verify(catalog, never()).reserve(any(), anyList());
        verify(orders, never()).place(any());
        verifyNoInteractions(payments);
    }

    @Test
    void placeOrder_whenOrderWriteFails_releasesReservation() {
        when(orders.findByIdempotencyKey(any(), anyString())).thenReturn(Optional.empty());
        when(catalog.reserve(any(), anyList())).thenReturn(new ReservationOutcome(null, "RESERVED", Instant.now()));
        when(orders.place(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.placeOrder(request(List.of(line(RING, 1, null)), "COD", null), "key-4"))
                .isInstanceOf(IllegalStateException.class);
        ArgumentCaptor<UUID> reservedId = ArgumentCaptor.forClass(UUID.class);
        verify(catalog).reserve(reservedId.capture(), anyList());
        verify(catalog).release(reservedId.getValue());
    }

    @Test
    void placeOrder_withVoucher_appliesDiscount_andRecordsUsage() {
        when(orders.findByIdempotencyKey(any(), anyString())).thenReturn(Optional.empty());
        UUID voucherId = UUID.randomUUID();
        when(promotions.quote("SALE10", CUSTOMER, new BigDecimal("450000"))).thenReturn(new DiscountQuote(voucherId, "SALE10", new BigDecimal("45000"), null));
        when(catalog.reserve(any(), anyList())).thenReturn(new ReservationOutcome(null, "RESERVED", Instant.now()));
        when(orders.place(any())).thenAnswer(inv -> orderView(inv.getArgument(0)));
        when(payments.initiate(any(), any(), any(), any())).thenReturn(new PaymentInitResult(UUID.randomUUID(), PaymentMethod.COD, "INIT", null));

        service.placeOrder(request(List.of(line(RING, 1, null)), "COD", "SALE10"), "key-5");

        ArgumentCaptor<PlaceOrderCommand> placed = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(orders).place(placed.capture());
        assertThat(placed.getValue().discount()).isEqualByComparingTo("45000");
        assertThat(placed.getValue().total()).isEqualByComparingTo("430000");
        verify(promotions).recordUsage(eq(voucherId), any(), eq(CUSTOMER), eq(new BigDecimal("45000")));
    }

    @Test
    void placeOrder_addressFromBook_isFetchedFromCustomerService_notTrustedFromClient() {
        when(orders.findByIdempotencyKey(any(), anyString())).thenReturn(Optional.empty());
        UUID addressId = UUID.randomUUID();
        when(customers.findAddress(CUSTOMER, addressId)).thenReturn(Optional.empty());

        CheckoutRequest request = new CheckoutRequest(List.of(line(RING, 1, null)), addressId, null, "COD", "GHN", null, null, null);
        assertThatThrownBy(() -> service.placeOrder(request, "key-6"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(CheckoutErrorKeys.ADDRESS_NOT_FOUND);
        verify(orders, never()).place(any());
    }

    @Test
    void quote_invalidVoucher_isReportedNotThrown() {
        when(promotions.quote(eq("BAD"), any(), any())).thenThrow(new BadRequestException("promotion.voucher-expired"));

        CheckoutQuoteView quote = service.quote(request(List.of(line(RING, 2, null)), null, "BAD"));

        assertThat(quote.subtotal()).isEqualByComparingTo("900000");
        assertThat(quote.discount()).isEqualByComparingTo("0");
        assertThat(quote.voucherError()).isEqualTo("promotion.voucher-expired");
        assertThat(quote.shippingOptions()).hasSize(1);
        assertThat(quote.total()).isEqualByComparingTo("925000");
    }

    private static CheckoutRequest request(List<CheckoutRequest.Line> lines, String paymentMethod, String voucher) {
        CheckoutRequest.AddressInput address = new CheckoutRequest.AddressInput("Lan", "0912345678", "01", "Thành phố Hà Nội", "00004", "Phường Ba Đình", "1 Hàng Bài", null);
        return new CheckoutRequest(lines, null, address, paymentMethod, "GHN", voucher, null, "Giao giờ hành chính");
    }

    private static CheckoutRequest.Line line(UUID sku, int qty, String engraving) {
        return new CheckoutRequest.Line(sku, qty, engraving);
    }

    private static SkuSnapshot sku(UUID id, String name, String price, int available, boolean sellable) {
        return new SkuSnapshot(id, UUID.randomUUID(), "slug", name, "Bạc / 6", "SKU", new BigDecimal(price), null, null, available, sellable);
    }

    private static OrderView orderView(PlaceOrderCommand cmd) {
        return new OrderView(cmd.orderId(), "ORD-261020-0001", cmd.confirmImmediately() ? "CONFIRMED" : "AWAITING_PAYMENT", cmd.subtotal(),
                cmd.discount(), cmd.shippingFee(), cmd.total(), cmd.voucherCode(), cmd.paymentMethod(), cmd.shippingCarrier(),
                (ShippingAddress) cmd.address(), cmd.contactEmail(), cmd.note(), Instant.now(), null, null, null, List.of(), List.of(), null, 0);
    }
}
