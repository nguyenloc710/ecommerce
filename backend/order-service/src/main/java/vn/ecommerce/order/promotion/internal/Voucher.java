package vn.ecommerce.order.promotion.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import vn.ecommerce.common.jpa.SoftDeletableEntity;

/** Mã giảm giá; xóa mềm vì đơn cũ còn tham chiếu qua {@code voucher_usage}. */
@Getter
@Setter
@Entity
@Table(name = "voucher")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Voucher extends SoftDeletableEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoucherType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Column(name = "max_discount", precision = 19, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal minSubtotal = BigDecimal.ZERO;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "per_customer_limit", nullable = false)
    private int perCustomerLimit = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoucherStatus status = VoucherStatus.ACTIVE;

    private String description;

    public Voucher(String code) {
        this.code = code;
    }

    /** Số tiền giảm cho tạm tính: % có trần {@code maxDiscount}, cố định không vượt tạm tính; làm tròn xuống đồng. */
    public BigDecimal discountFor(BigDecimal subtotal) {
        BigDecimal raw = type == VoucherType.PERCENT
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                : value;
        if (type == VoucherType.PERCENT && maxDiscount != null) {
            raw = raw.min(maxDiscount);
        }
        return raw.min(subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.DOWN);
    }

    public boolean isExhausted() {
        return usageLimit != null && usedCount >= usageLimit;
    }

    public void incrementUsed() {
        this.usedCount++;
    }

    public enum VoucherType {
        PERCENT,
        FIXED
    }

    public enum VoucherStatus {
        ACTIVE,
        INACTIVE
    }
}
