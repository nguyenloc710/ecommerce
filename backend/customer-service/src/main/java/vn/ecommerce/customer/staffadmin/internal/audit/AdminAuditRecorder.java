package vn.ecommerce.customer.staffadmin.internal.audit;

import java.util.Map;

/** Ghi {@code admin_audit_log} trong cùng transaction với thao tác nghiệp vụ. */
public interface AdminAuditRecorder {

    String TARGET_CUSTOMER = "CUSTOMER";
    String TARGET_STAFF = "STAFF";

    void record(String action, String targetType, String targetId, Map<String, Object> details);
}
