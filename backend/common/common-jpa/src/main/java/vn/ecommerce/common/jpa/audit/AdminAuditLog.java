package vn.ecommerce.common.jpa.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bản ghi bất biến về thao tác của nhân viên (khóa khách, đổi trạng thái đơn, tạo voucher...); chỉ thêm, không sửa/xóa.
 * Mỗi service có bảng {@code admin_audit_log} riêng với cùng cấu trúc (migration của service tự tạo).
 */
@Getter
@Entity
@Table(name = "admin_audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    private UUID id;

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AdminAuditLog(String actorId, String action, String targetType, String targetId, Map<String, Object> details,
                         String traceId) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.traceId = traceId;
        this.createdAt = Instant.now();
    }
}
