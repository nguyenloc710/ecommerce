package vn.ecommerce.customer.address.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import vn.ecommerce.common.jpa.SoftDeletableEntity;

/** Địa chỉ giao hàng của khách; xóa mềm vì đơn hàng cũ còn tham chiếu. */
@Getter
@Setter
@Entity
@Table(name = "address")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends SoftDeletableEntity {

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "province_code", nullable = false, length = 10)
    private String provinceCode;

    @Column(name = "province_name", nullable = false, length = 120)
    private String provinceName;

    @Column(name = "ward_code", nullable = false, length = 10)
    private String wardCode;

    @Column(name = "ward_name", nullable = false, length = 120)
    private String wardName;

    @Column(nullable = false)
    private String line1;

    private String note;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    public Address(UUID customerId) {
        this.customerId = customerId;
    }
}
