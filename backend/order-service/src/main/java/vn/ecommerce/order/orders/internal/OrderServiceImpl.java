package vn.ecommerce.order.orders.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.orders.OrderStatus;
import vn.ecommerce.order.orders.OrderSummaryView;
import vn.ecommerce.order.orders.OrderView;
import vn.ecommerce.order.orders.PlaceOrderCommand;
import vn.ecommerce.order.shipping.ShipmentStatusChanged;
import vn.ecommerce.order.shipping.ShippingApi;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class OrderServiceImpl implements OrderService {

    static final String REASON_PAYMENT_TIMEOUT = "payment-timeout";
    static final String REASON_PAYMENT_FAILED_PREFIX = "payment-failed";
    static final String REASON_CUSTOMER_CANCEL = "customer-cancel";

    private final OrderRepository orders;
    private final OrderCodeGenerator codes;
    private final OrderStateMachine stateMachine;
    private final OrderViewMapper mapper;
    private final ShippingApi shipping;
    private final OrderProperties properties;
    private final CurrentUser currentUser;

    @Override
    public OrderView place(PlaceOrderCommand cmd) {
        Order order = new Order(cmd.orderId(), codes.next(), cmd.customerId(), cmd.idempotencyKey(), cmd.subtotal(), cmd.discount(),
                cmd.shippingFee(), cmd.voucherCode(), cmd.address().recipientName(), cmd.address().phone(), cmd.contactEmail(),
                mapper.toMap(cmd.address()), cmd.note(), cmd.paymentMethod(), cmd.shippingCarrier(), stateMachine.actor());
        cmd.lines().forEach(l -> order.addItem(l.skuId(), l.productId(), l.productSlug(), l.productName(), l.variantLabel(), l.skuCode(),
                l.imageUrl(), l.unitPrice(), l.quantity(), l.engraving()));
        stateMachine.transition(order, cmd.confirmImmediately() ? OrderStatus.CONFIRMED : OrderStatus.AWAITING_PAYMENT, null);
        Order saved = orders.save(order);
        log.info("Tạo đơn {} cho khách {}: {} dòng, tổng {}", saved.getCode(), saved.getCustomerId(), saved.getItems().size(), saved.getTotal());
        return mapper.toView(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> findByIdempotencyKey(UUID customerId, String idempotencyKey) {
        return orders.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey).map(this::view);
    }

    @Override
    public void markPaid(UUID orderId, String providerTxnId) {
        Order order = find(orderId);
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            stateMachine.transition(order, OrderStatus.CONFIRMED, "paid:" + providerTxnId);
            orders.save(order);
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            // B-21: tiền về sau khi đơn đã hủy (hết hạn) → cần hoàn tiền tay; không đổi trạng thái đơn
            log.warn("Đơn {} đã hủy nhưng nhận thanh toán {} → cần hoàn tiền", order.getCode(), providerTxnId);
        } else {
            log.info("Đơn {} đã ở {}; bỏ qua webhook thanh toán {} (idempotent)", order.getCode(), order.getStatus(), providerTxnId);
        }
    }

    @Override
    public void markPaymentFailed(UUID orderId, String reason) {
        Order order = find(orderId);
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            stateMachine.transition(order, OrderStatus.CANCELLED, REASON_PAYMENT_FAILED_PREFIX + ":" + reason);
            orders.save(order);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryView> listMine(int page, int size) {
        Page<Order> result = orders.findByCustomerIdOrderByPlacedAtDesc(currentId(), PageRequest.of(page, size));
        return PageResponse.of(result.map(mapper::toSummary).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView getMine(String code) {
        return view(mine(code));
    }

    @Override
    public OrderView cancelMine(String code, String reason) {
        Order order = mine(code);
        if (!order.getStatus().customerCancellable()) {
            throw new ConflictException(OrderErrorKeys.CANNOT_CANCEL, order.getStatus().name());
        }
        stateMachine.transition(order, OrderStatus.CANCELLED, reason == null || reason.isBlank() ? REASON_CUSTOMER_CANCEL : reason);
        return view(orders.save(order));
    }

    @Override
    public void onShipmentStatus(ShipmentStatusChanged event) {
        if (!ShipmentStatusChanged.DELIVERED.equals(event.status())) {
            return;
        }
        Order order = find(event.orderId());
        if (order.getStatus() == OrderStatus.SHIPPING) {
            stateMachine.transition(order, OrderStatus.DELIVERED, "carrier:" + event.carrier());
            orders.save(order);
        }
    }

    @Override
    public int cancelExpiredAwaitingPayment() {
        Instant cutoff = Instant.now().minus(properties.paymentTimeout());
        List<Order> expired = orders.findTop200ByStatusAndPlacedAtBefore(OrderStatus.AWAITING_PAYMENT, cutoff);
        expired.forEach(order -> stateMachine.transition(order, OrderStatus.CANCELLED, REASON_PAYMENT_TIMEOUT));
        orders.saveAll(expired);
        return expired.size();
    }

    private Order mine(String code) {
        return orders.findByCodeAndCustomerId(code, currentId()).orElseThrow(() -> new NotFoundException(OrderErrorKeys.NOT_FOUND));
    }

    private Order find(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new NotFoundException(OrderErrorKeys.NOT_FOUND));
    }

    private UUID currentId() {
        return UUID.fromString(currentUser.requireId());
    }

    private OrderView view(Order order) {
        return mapper.toView(order, shipping.findByOrder(order.getId()).orElse(null));
    }
}
