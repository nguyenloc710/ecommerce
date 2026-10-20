package vn.ecommerce.customer.address.internal.division;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Đơn vị hành chính 2 cấp; dữ liệu tham chiếu nạp bằng Flyway, ứng dụng chỉ đọc. */
@Getter
@Entity
@Table(name = "admin_division")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDivision {

    public static final short LEVEL_PROVINCE = 1;
    public static final short LEVEL_WARD = 2;

    @Id
    @Column(length = 10)
    private String code;

    @Column(name = "parent_code", length = 10)
    private String parentCode;

    @Column(nullable = false)
    private short level;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "division_type", nullable = false, length = 40)
    private String divisionType;

    @Column(nullable = false, length = 120)
    private String codename;
}
