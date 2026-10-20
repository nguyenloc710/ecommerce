package vn.ecommerce.common.jpa.audit;

import java.util.Map;

/**
 * Ghi {@code admin_audit_log} trong cùng transaction với thao tác nghiệp vụ (propagation MANDATORY:
 * gọi ngoài transaction là lỗi lập trình). Bean mặc định do {@code JpaAuditingAutoConfiguration} tạo.
 */
public interface AdminAuditRecorder {

    void record(String action, String targetType, String targetId, Map<String, Object> details);
}
