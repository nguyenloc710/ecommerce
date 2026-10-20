package vn.ecommerce.order.checkout.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.ApiException;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.order.cart.CartApi;
import vn.ecommerce.order.catalogclient.CatalogClient;
import vn.ecommerce.order.checkout.CheckoutQuoteView;
import vn.ecommerce.order.checkout.CheckoutResult;
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

/**
 * Orchestrator: các bước gọi service khác (catalog, customer) chạy ngoài transaction DB; phần ghi đơn + voucher +
 * payment + dọn giỏ nằm trong một transaction. Lỗi sau khi đã giữ hàng → nhả hàng ngay (và TTL vẫn là lưới an toàn).
 */
@Slf4j
@Service
@RequiredArgsConstructor
class CheckoutServiceImpl implements CheckoutService {

    private final PricingStep pricing;
    private final AddressResolver addresses;
    private final PromotionApi promotions;
    private final ShippingApi shipping;
    private final CatalogClient catalog;
    private final OrdersApi orders;
    private final PaymentApi payments;
    private final CartApi cart;
    private final CurrentUser currentUser;
    private final TransactionTemplate transactions;

    @Override
    public CheckoutQuoteView quote(CheckoutRequest request) {
        UUID customerId = currentId();
        PricedLines priced = pricing.price(request.lines());
        String provinceCode = addresses.resolve(customerId, request).map(ShippingAddress::provinceCode).orElse(null);

        BigDecimal discount = BigDecimal.ZERO;
        String voucherError = null;
        if (hasText(request.voucherCode())) {
            try {
                discount = promotions.quote(request.voucherCode(), customerId, priced.subtotal()).discount();
            } catch (ApiException e) {
                voucherError = e.getMessageKey();
            }
        }
        List<ShippingOption> options = shipping.options(priced.subtotal(), provinceCode);
        ShippingOption selected = hasText(request.shippingCarrier())
                ? shipping.option(request.shippingCarrier(), priced.subtotal(), provinceCode)
                : options.isEmpty() ? null : options.getFirst();
        BigDecimal fee = selected == null ? BigDecimal.ZERO : selected.fee();
        return new CheckoutQuoteView(priced.toQuoteLines(), priced.subtotal(), hasText(request.voucherCode()) ? request.voucherCode() : null,
                discount, voucherError, options, selected, fee, priced.subtotal().subtract(discount).add(fee));
    }

    @Override
    public CheckoutResult placeOrder(CheckoutRequest request, String idempotencyKey) {
        if (!hasText(idempotencyKey)) {
            throw new BadRequestException(CheckoutErrorKeys.IDEMPOTENCY_KEY_REQUIRED);
        }
        UUID customerId = currentId();
        Optional<OrderView> existing = orders.findByIdempotencyKey(customerId, idempotencyKey);
        if (existing.isPresent()) {
            return new CheckoutResult(existing.get(), null, true);
        }
        if (!hasText(request.paymentMethod())) {
            throw new BadRequestException(CheckoutErrorKeys.PAYMENT_METHOD_REQUIRED);
        }
        PaymentMethod method = PaymentMethod.valueOf(request.paymentMethod());
        ShippingAddress address = addresses.require(customerId, request);
        PricedLines priced = pricing.price(request.lines());
        DiscountQuote voucher = hasText(request.voucherCode()) ? promotions.quote(request.voucherCode(), customerId, priced.subtotal()) : null;
        ShippingOption option = shipping.option(hasText(request.shippingCarrier()) ? request.shippingCarrier() : defaultCarrier(priced, address),
                priced.subtotal(), address.provinceCode());
        String contactEmail = hasText(request.contactEmail()) ? request.contactEmail().trim() : currentUser.email().orElse("");

        UUID orderId = UUID.randomUUID();
        MDC.put("orderId", orderId.toString());
        try {
            catalog.reserve(orderId, priced.toReserveLines());
            PlaceOrderCommand command = new PlaceOrderCommand(orderId, customerId, idempotencyKey, priced.toOrderLines(), priced.subtotal(),
                    voucher == null ? BigDecimal.ZERO : voucher.discount(), voucher == null ? null : voucher.code(), option.fee(), option.carrier(),
                    method.name(), !method.online(), address, contactEmail, request.note());
            Placed placed = transactions.execute(tx -> {
                OrderView order = orders.place(command);
                if (voucher != null) {
                    promotions.recordUsage(voucher.voucherId(), orderId, customerId, voucher.discount());
                }
                PaymentInitResult payment = payments.initiate(orderId, order.code(), order.total(), method);
                cart.removeLines(customerId, priced.skuIds());
                return new Placed(order, payment);
            });
            log.info("Đặt hàng thành công {} ({}, {})", placed.order().code(), method, placed.order().total());
            return new CheckoutResult(placed.order(), placed.payment().payUrl(), false);
        } catch (RuntimeException e) {
            // Giữ hàng rồi mà không ghi được đơn: nhả ngay để khách khác mua được, không chờ TTL
            catalog.release(orderId);
            throw e;
        } finally {
            MDC.remove("orderId");
        }
    }

    private String defaultCarrier(PricedLines priced, ShippingAddress address) {
        List<ShippingOption> options = shipping.options(priced.subtotal(), address.provinceCode());
        if (options.isEmpty()) {
            throw new BadRequestException("shipping.carrier-not-found", "");
        }
        return options.getFirst().carrier();
    }

    private UUID currentId() {
        return UUID.fromString(currentUser.requireId());
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private record Placed(OrderView order, PaymentInitResult payment) {
    }
}
