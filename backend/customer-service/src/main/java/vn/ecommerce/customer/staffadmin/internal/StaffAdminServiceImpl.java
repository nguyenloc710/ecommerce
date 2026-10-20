package vn.ecommerce.customer.staffadmin.internal;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.customer.profile.CustomerStatus;
import vn.ecommerce.customer.profile.ProfileApi;
import vn.ecommerce.customer.staffadmin.internal.audit.AdminAuditRecorder;
import vn.ecommerce.customer.staffadmin.internal.client.KeycloakAdminGateway;
import vn.ecommerce.customer.staffadmin.internal.client.KeycloakUser;
import vn.ecommerce.customer.staffadmin.internal.dto.LockCommand;
import vn.ecommerce.customer.staffadmin.internal.dto.StaffCreateCommand;
import vn.ecommerce.customer.staffadmin.internal.dto.StaffView;

/**
 * Gọi Keycloak bên trong transaction: Keycloak lỗi thì rollback thay đổi DB, không để trạng thái lệch nhau.
 * Chiều ngược lại (DB lỗi sau khi Keycloak đã đổi) hiếm và admin thấy ngay qua trạng thái hồ sơ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class StaffAdminServiceImpl implements StaffAdminService {

    static final String ACTION_CUSTOMER_LOCK = "CUSTOMER_LOCK";
    static final String ACTION_CUSTOMER_UNLOCK = "CUSTOMER_UNLOCK";
    static final String ACTION_STAFF_CREATE = "STAFF_CREATE";
    static final String ACTION_STAFF_ENABLE = "STAFF_ENABLE";
    static final String ACTION_STAFF_DISABLE = "STAFF_DISABLE";

    private final KeycloakAdminGateway keycloak;
    private final ProfileApi profileApi;
    private final AdminAuditRecorder audit;
    private final CurrentUser currentUser;

    @Override
    public void lockCustomer(UUID customerId, LockCommand command) {
        if (customerId.toString().equals(currentUser.requireId())) {
            throw new BadRequestException(StaffAdminErrorKeys.CANNOT_LOCK_SELF);
        }
        profileApi.setStatus(customerId, CustomerStatus.LOCKED);
        keycloak.setEnabled(customerId.toString(), false);
        keycloak.logoutAllSessions(customerId.toString());
        Map<String, Object> details = new HashMap<>();
        details.put("reason", command == null ? null : command.reason());
        audit.record(ACTION_CUSTOMER_LOCK, AdminAuditRecorder.TARGET_CUSTOMER, customerId.toString(), details);
        log.info("Khóa khách hàng {}", customerId);
    }

    @Override
    public void unlockCustomer(UUID customerId) {
        profileApi.setStatus(customerId, CustomerStatus.ACTIVE);
        keycloak.setEnabled(customerId.toString(), true);
        audit.record(ACTION_CUSTOMER_UNLOCK, AdminAuditRecorder.TARGET_CUSTOMER, customerId.toString(), Map.of());
        log.info("Mở khóa khách hàng {}", customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffView> listStaff(int page, int size) {
        return keycloak.listStaff(page * size, size).stream().map(StaffAdminServiceImpl::toView).toList();
    }

    @Override
    public StaffView createStaff(StaffCreateCommand command) {
        String email = command.email().trim().toLowerCase();
        String id = keycloak.createStaff(email, command.fullName().trim(), command.temporaryPassword());
        audit.record(ACTION_STAFF_CREATE, AdminAuditRecorder.TARGET_STAFF, id, Map.of("email", email));
        return keycloak.findUser(id).map(StaffAdminServiceImpl::toView)
                .orElseGet(() -> new StaffView(id, email, command.fullName().trim(), true, Instant.now()));
    }

    @Override
    public void setStaffEnabled(String staffId, boolean enabled) {
        KeycloakUser user = keycloak.findUser(staffId).orElseThrow(() -> new NotFoundException(StaffAdminErrorKeys.USER_NOT_FOUND));
        keycloak.setEnabled(user.id(), enabled);
        if (!enabled) {
            keycloak.logoutAllSessions(user.id());
        }
        audit.record(enabled ? ACTION_STAFF_ENABLE : ACTION_STAFF_DISABLE, AdminAuditRecorder.TARGET_STAFF, user.id(),
                Map.of("email", user.email() == null ? "" : user.email()));
    }

    private static StaffView toView(KeycloakUser user) {
        Instant createdAt = user.createdTimestamp() == null ? null : Instant.ofEpochMilli(user.createdTimestamp());
        return new StaffView(user.id(), user.email(), user.fullName(), user.enabled(), createdAt);
    }
}
