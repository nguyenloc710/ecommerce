package vn.ecommerce.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Một revision của Envers = một transaction có thay đổi entity {@code @Audited}.
 * Lưu thêm {@code username} để biết ai đổi. Bảng {@code revinfo} do Flyway của từng service tạo.
 */
@Getter
@Setter
@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevisionEntity {

    @Id
    @RevisionNumber
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @RevisionTimestamp
    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "username", nullable = false, length = 64)
    private String username;
}
