package vn.ecommerce.catalog.product.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ecommerce.common.jpa.AuditableEntity;

/** Một lần nhập giá vàng (chỉ thêm, không sửa): bản ghi mới nhất của mỗi loại vàng là giá hiện hành. */
@Getter
@Entity
@Table(name = "gold_rate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoldRate extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private GoldKarat karat;

    @Column(name = "price_per_gram", nullable = false, precision = 19, scale = 2)
    private BigDecimal pricePerGram;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(length = 255)
    private String note;

    public GoldRate(GoldKarat karat, BigDecimal pricePerGram, Instant effectiveAt, String note) {
        this.karat = karat;
        this.pricePerGram = pricePerGram;
        this.effectiveAt = effectiveAt;
        this.note = note;
    }
}
