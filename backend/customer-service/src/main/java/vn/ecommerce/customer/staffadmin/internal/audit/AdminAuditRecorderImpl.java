package vn.ecommerce.customer.staffadmin.internal.audit;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.security.CurrentUser;

@Component
@RequiredArgsConstructor
class AdminAuditRecorderImpl implements AdminAuditRecorder {

    /** Cùng key với common-web (TraceIdResponseFilter) để tra ngược log theo traceId. */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    private final AdminAuditLogRepository repository;
    private final CurrentUser currentUser;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String targetType, String targetId, Map<String, Object> details) {
        repository.save(new AdminAuditLog(currentUser.requireId(), action, targetType, targetId, details,
                MDC.get(TRACE_ID_MDC_KEY)));
    }
}
