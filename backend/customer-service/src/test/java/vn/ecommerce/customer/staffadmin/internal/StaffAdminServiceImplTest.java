package vn.ecommerce.customer.staffadmin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.customer.profile.CustomerStatus;
import vn.ecommerce.customer.profile.ProfileApi;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.customer.staffadmin.internal.client.KeycloakAdminGateway;
import vn.ecommerce.customer.staffadmin.internal.client.KeycloakUnavailableException;
import vn.ecommerce.customer.staffadmin.internal.client.KeycloakUser;
import vn.ecommerce.customer.staffadmin.internal.dto.LockCommand;
import vn.ecommerce.customer.staffadmin.internal.dto.StaffCreateCommand;
import vn.ecommerce.customer.staffadmin.internal.dto.StaffView;

@ExtendWith(MockitoExtension.class)
class StaffAdminServiceImplTest {

    private static final String ADMIN = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final UUID CUSTOMER = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Mock KeycloakAdminGateway keycloak;
    @Mock ProfileApi profileApi;
    @Mock AdminAuditRecorder audit;
    @Mock CurrentUser currentUser;

    private StaffAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StaffAdminServiceImpl(keycloak, profileApi, audit, currentUser);
    }

    @Test
    void lockCustomer_updatesProfile_disablesInKeycloak_logsOut_andAudits() {
        when(currentUser.requireId()).thenReturn(ADMIN);

        service.lockCustomer(CUSTOMER, new LockCommand("Gian lận"));

        verify(profileApi).setStatus(CUSTOMER, CustomerStatus.LOCKED);
        verify(keycloak).setEnabled(CUSTOMER.toString(), false);
        verify(keycloak).logoutAllSessions(CUSTOMER.toString());
        verify(audit).record(eq(StaffAdminServiceImpl.ACTION_CUSTOMER_LOCK), eq(StaffAdminServiceImpl.TARGET_CUSTOMER),
                eq(CUSTOMER.toString()), eq(mapWithReason("Gian lận")));
    }

    @Test
    void lockCustomer_self_isBadRequest_andNothingChanges() {
        when(currentUser.requireId()).thenReturn(CUSTOMER.toString());

        assertThatThrownBy(() -> service.lockCustomer(CUSTOMER, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(StaffAdminErrorKeys.CANNOT_LOCK_SELF);
        verifyNoInteractions(profileApi, keycloak, audit);
    }

    @Test
    void lockCustomer_unknownProfile_isNotFound_beforeTouchingKeycloak() {
        when(currentUser.requireId()).thenReturn(ADMIN);
        doThrow(new NotFoundException("customer.not-found")).when(profileApi).setStatus(CUSTOMER, CustomerStatus.LOCKED);

        assertThatThrownBy(() -> service.lockCustomer(CUSTOMER, null)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(keycloak, audit);
    }

    @Test
    void lockCustomer_keycloakDown_propagates_soTransactionRollsBack_andNoAudit() {
        when(currentUser.requireId()).thenReturn(ADMIN);
        doThrow(new KeycloakUnavailableException(StaffAdminErrorKeys.KEYCLOAK_UNAVAILABLE))
                .when(keycloak).setEnabled(anyString(), anyBoolean());

        assertThatThrownBy(() -> service.lockCustomer(CUSTOMER, null)).isInstanceOf(KeycloakUnavailableException.class);
        verify(audit, never()).record(any(), any(), any(), any());
    }

    @Test
    void unlockCustomer_reactivatesProfile_andEnablesInKeycloak() {
        service.unlockCustomer(CUSTOMER);

        verify(profileApi).setStatus(CUSTOMER, CustomerStatus.ACTIVE);
        verify(keycloak).setEnabled(CUSTOMER.toString(), true);
        verify(keycloak, never()).logoutAllSessions(any());
        verify(audit).record(eq(StaffAdminServiceImpl.ACTION_CUSTOMER_UNLOCK), any(), eq(CUSTOMER.toString()), any());
    }

    @Test
    void createStaff_normalizesEmail_audits_andReturnsKeycloakView() {
        when(keycloak.createStaff("minh@shop.vn", "Trần Minh", "Temp#12345")).thenReturn("kc-1");
        when(keycloak.findUser("kc-1")).thenReturn(Optional.of(
                new KeycloakUser("kc-1", "minh@shop.vn", "minh@shop.vn", "Trần", "Minh", true, 1_700_000_000_000L)));

        StaffView view = service.createStaff(new StaffCreateCommand(" Minh@Shop.vn ", " Trần Minh ", "Temp#12345"));

        assertThat(view.id()).isEqualTo("kc-1");
        assertThat(view.fullName()).isEqualTo("Trần Minh");
        assertThat(view.enabled()).isTrue();
        assertThat(view.createdAt()).isNotNull();
        verify(audit).record(StaffAdminServiceImpl.ACTION_STAFF_CREATE, StaffAdminServiceImpl.TARGET_STAFF, "kc-1",
                Map.of("email", "minh@shop.vn"));
    }

    @Test
    void setStaffEnabled_unknownUser_isNotFound_andNoAudit() {
        when(keycloak.findUser("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setStaffEnabled("ghost", false)).isInstanceOf(NotFoundException.class);
        verify(keycloak, never()).setEnabled(any(), anyBoolean());
        verifyNoInteractions(audit);
    }

    @Test
    void setStaffEnabled_disable_alsoLogsOutSessions() {
        when(keycloak.findUser("kc-2")).thenReturn(Optional.of(new KeycloakUser("kc-2", "a@shop.vn", "a@shop.vn", "A", "B", true, null)));

        service.setStaffEnabled("kc-2", false);

        verify(keycloak).setEnabled("kc-2", false);
        verify(keycloak).logoutAllSessions("kc-2");
        verify(audit).record(eq(StaffAdminServiceImpl.ACTION_STAFF_DISABLE), eq(StaffAdminServiceImpl.TARGET_STAFF), eq("kc-2"), any());
    }

    @Test
    void listStaff_mapsPagingToKeycloakFirstMax() {
        when(keycloak.listStaff(40, 20)).thenReturn(List.of());

        assertThat(service.listStaff(2, 20)).isEmpty();
    }

    private static Map<String, Object> mapWithReason(String reason) {
        return new java.util.HashMap<>(Map.of("reason", reason));
    }
}
