package vn.ecommerce.order.orders.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.web.error.ApiException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.orders.OrderStatus;
import vn.ecommerce.order.orders.OrderSummaryView;
import vn.ecommerce.order.orders.OrderView;
import vn.ecommerce.order.shipping.ShipmentView;
import vn.ecommerce.order.shipping.ShippingApi;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class OrderAdminServiceImpl implements OrderAdminService {

    static final String TARGET_ORDER = "ORDER";

    private final OrderRepository orders;
    private final OrderStateMachine stateMachine;
    private final OrderViewMapper mapper;
    private final ShippingApi shipping;
    private final AdminAuditRecorder audit;
    private final TransactionTemplate transactions;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryView> search(OrderStatus status, String q, int page, int size) {
        String pattern = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        Page<Order> result = orders.search(status, pattern, PageRequest.of(page, size));
        return PageResponse.of(result.map(mapper::toSummary).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView get(String code) {
        return view(find(code));
    }

    @Override
    public OrderView changeStatus(String code, OrderStatus to, String reason) {
        if (to == OrderStatus.SHIPPING || to == OrderStatus.CANCELLED) {
            // Hai bước này có nghiệp vụ riêng (tạo vận đơn / nhả hàng + lý do bắt buộc)
            throw new ConflictException(OrderErrorKeys.INVALID_TRANSITION, "manual", to.name());
        }
        Order order = find(code);
        OrderStatus from = order.getStatus();
        stateMachine.transition(order, to, reason);
        audit.record("ORDER_STATUS", TARGET_ORDER, code, Map.of("from", from.name(), "to", to.name(), "reason", reason == null ? "" : reason));
        return view(orders.save(order));
    }

    @Override
    public OrderView ship(String code, String carrier) {
        Order order = find(code);
        // Bỏ qua PACKING nếu nhân viên tạo vận đơn thẳng từ CONFIRMED
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            stateMachine.transition(order, OrderStatus.PACKING, "auto-before-ship");
        }
        if (!order.getStatus().canTransitionTo(OrderStatus.SHIPPING)) {
            throw new ConflictException(OrderErrorKeys.INVALID_TRANSITION, order.getStatus().name(), OrderStatus.SHIPPING.name());
        }
        String carrierCode = carrier == null || carrier.isBlank() ? order.getShippingCarrier() : carrier;
        ShipmentView shipment = shipping.createShipment(order.getId(), carrierCode, order.getShippingFee());
        stateMachine.transition(order, OrderStatus.SHIPPING, "tracking:" + shipment.trackingCode(), shipment.carrier(), shipment.trackingCode());
        audit.record("ORDER_SHIP", TARGET_ORDER, code, Map.of("carrier", shipment.carrier(), "trackingCode", shipment.trackingCode()));
        return mapper.toView(orders.save(order), shipment);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BulkShipResult bulkShip(List<String> codes, String carrier) {
        // Mỗi đơn một transaction riêng để một đơn lỗi không kéo cả lô
        List<String> shipped = new ArrayList<>();
        List<BulkShipResult.Failure> failed = new ArrayList<>();
        for (String code : codes) {
            try {
                transactions.executeWithoutResult(tx -> ship(code, carrier));
                shipped.add(code);
            } catch (ApiException e) {
                failed.add(new BulkShipResult.Failure(code, e.getMessageKey()));
            }
        }
        return new BulkShipResult(shipped, failed);
    }

    @Override
    public OrderView cancel(String code, String reason) {
        Order order = find(code);
        stateMachine.transition(order, OrderStatus.CANCELLED, reason);
        audit.record("ORDER_CANCEL", TARGET_ORDER, code, Map.of("reason", reason == null ? "" : reason));
        return view(orders.save(order));
    }

    private Order find(String code) {
        return orders.findByCode(code).orElseThrow(() -> new NotFoundException(OrderErrorKeys.NOT_FOUND));
    }

    private OrderView view(Order order) {
        return mapper.toView(order, shipping.findByOrder(order.getId()).orElse(null));
    }
}
