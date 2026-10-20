package vn.ecommerce.order.payment.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;
import vn.ecommerce.order.payment.PaymentMethod;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Một giao dịch thanh toán của đơn; webhook idempotent theo (provider, provider_txn_id). */
@Getter
@Entity
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends AuditableEntity {

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentMethod provider;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "provider_txn_id", length = 100)
    private String providerTxnId;

    @Column(name = "pay_url", length = 1000)
    private String payUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_callback")
    private Map<String, Object> rawCallback;

    @Column(name = "paid_at")
    private Instant paidAt;

    Payment(UUID orderId, PaymentMethod provider, BigDecimal amount, PaymentStatus status, String payUrl) {
        this.orderId = orderId;
        this.provider = provider;
        this.amount = amount;
        this.status = status;
        this.payUrl = payUrl;
    }

    void settle(boolean success, String txnId, Map<String, Object> raw) {
        this.status = success ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        this.providerTxnId = txnId;
        this.rawCallback = raw;
        this.paidAt = success ? Instant.now() : null;
    }

    public boolean isSettled() {
        return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED;
    }

    public enum PaymentStatus {
        /** COD: thu khi giao, không có giao dịch online. */
        INIT,
        /** Đã có URL, chờ khách thanh toán. */
        PENDING,
        SUCCEEDED,
        FAILED
    }
}
