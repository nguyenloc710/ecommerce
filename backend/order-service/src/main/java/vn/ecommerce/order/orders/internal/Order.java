package vn.ecommerce.order.orders.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.ecommerce.common.jpa.AuditableEntity;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.order.orders.OrderStatus;

/** Đơn hàng: mọi số liệu là snapshot lúc đặt; đổi trạng thái chỉ qua {@link #transitionTo}. */
@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends AuditableEntity {

    @Column(nullable = false, updatable = false, length = 20)
    private String code;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "voucher_code", length = 30)
    private String voucherCode;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false)
    private Map<String, Object> shippingAddress;

    @Column(length = 500)
    private String note;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "shipping_carrier", nullable = false, length = 20)
    private String shippingCarrier;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    Order(UUID id, String code, UUID customerId, String idempotencyKey, BigDecimal subtotal, BigDecimal discount,
          BigDecimal shippingFee, String voucherCode, String recipientName, String contactPhone, String contactEmail,
          Map<String, Object> shippingAddress, String note, String paymentMethod, String shippingCarrier, String actor) {
        assignId(id);
        this.code = code;
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.subtotal = subtotal;
        this.discount = discount;
        this.shippingFee = shippingFee;
        this.total = subtotal.subtract(discount).add(shippingFee);
        this.voucherCode = voucherCode;
        this.recipientName = recipientName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.paymentMethod = paymentMethod;
        this.shippingCarrier = shippingCarrier;
        this.placedAt = Instant.now();
        this.history.add(new OrderStatusHistory(this, null, OrderStatus.PENDING, null, actor));
    }

    OrderItem addItem(UUID skuId, UUID productId, String productSlug, String productName, String variantLabel, String skuCode,
                      String imageUrl, BigDecimal unitPrice, int quantity, String engraving) {
        OrderItem item = new OrderItem(this, skuId, productId, productSlug, productName, variantLabel, skuCode, imageUrl,
                unitPrice, quantity, engraving);
        items.add(item);
        return item;
    }

    /**
     * Chuyển trạng thái có kiểm tra và ghi lịch sử.
     *
     * @throws ConflictException {@code order.invalid-transition} nếu bảng trạng thái không cho phép
     */
    void transitionTo(OrderStatus target, String reason, String actor) {
        if (!status.canTransitionTo(target)) {
            throw new ConflictException(OrderErrorKeys.INVALID_TRANSITION, status.name(), target.name());
        }
        history.add(new OrderStatusHistory(this, status, target, reason, actor));
        this.status = target;
        if (target == OrderStatus.CANCELLED) {
            this.cancelledAt = Instant.now();
            this.cancelReason = reason;
        }
    }

    void markPaidAt(Instant at) {
        this.paidAt = at;
    }
}
