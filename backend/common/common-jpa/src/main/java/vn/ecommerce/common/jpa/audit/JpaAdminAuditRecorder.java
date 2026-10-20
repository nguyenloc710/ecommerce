package vn.ecommerce.common.jpa.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.security.CurrentUser;

@RequiredArgsConstructor
public class JpaAdminAuditRecorder implements AdminAuditRecorder {

    /** Cùng key với common-web (TraceIdResponseFilter) để tra ngược log theo traceId. */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    private final CurrentUser currentUser;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String targetType, String targetId, Map<String, Object> details) {
        entityManager.persist(new AdminAuditLog(currentUser.requireId(), action, targetType, targetId, details,
                MDC.get(TRACE_ID_MDC_KEY)));
    }
}
