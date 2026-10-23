package vn.ecommerce.catalog.flashsale.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Chương trình flash sale: khung giờ + danh sách {@link FlashSaleItem}. Không xóa; dừng sớm = CANCELLED. */
@Getter
@Entity
@Table(name = "flash_sale")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashSale extends AuditableEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FlashSaleStatus status = FlashSaleStatus.ACTIVE;

    public FlashSale(String name, Instant startsAt, Instant endsAt) {
        this.name = name;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public FlashSalePhase phase(Instant now) {
        if (status == FlashSaleStatus.CANCELLED) {
            return FlashSalePhase.CANCELLED;
        }
        if (now.isBefore(startsAt)) {
            return FlashSalePhase.UPCOMING;
        }
        return now.isBefore(endsAt) ? FlashSalePhase.RUNNING : FlashSalePhase.ENDED;
    }

    /** Trả false nếu đã hủy (gọi lại không ghi gì). Đơn đang giữ suất vẫn giữ giá đã báo. */
    public boolean cancel() {
        if (status == FlashSaleStatus.CANCELLED) {
            return false;
        }
        status = FlashSaleStatus.CANCELLED;
        return true;
    }
}
