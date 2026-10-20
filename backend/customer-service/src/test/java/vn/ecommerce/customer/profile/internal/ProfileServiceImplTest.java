package vn.ecommerce.customer.profile.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import vn.ecommerce.common.events.customer.CustomerCreatedEvent;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.customer.profile.CustomerProfileView;
import vn.ecommerce.customer.profile.CustomerStatus;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final UUID SUB = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock CustomerProfileRepository repository;
    @Mock CurrentUser currentUser;
    @Mock ApplicationEventPublisher events;

    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(repository, Mappers.getMapper(ProfileMapper.class), currentUser, events);
        lenient().when(currentUser.requireId()).thenReturn(SUB.toString());
    }

    @Test
    void ensureCurrentProfile_firstLogin_createsFromJwt_andPublishesEvent() {
        when(repository.findById(SUB)).thenReturn(Optional.empty());
        when(currentUser.email()).thenReturn(Optional.of("lan@example.com"));
        when(currentUser.displayName()).thenReturn(Optional.of("Lan Nguyễn"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerProfileView view = service.ensureCurrentProfile();

        assertThat(view.id()).isEqualTo(SUB);
        assertThat(view.email()).isEqualTo("lan@example.com");
        assertThat(view.status()).isEqualTo(CustomerStatus.ACTIVE.name());
        ArgumentCaptor<CustomerCreatedEvent> event = ArgumentCaptor.forClass(CustomerCreatedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().customerId()).isEqualTo(SUB.toString());
        assertThat(event.getValue().fullName()).isEqualTo("Lan Nguyễn");
    }

    @Test
    void ensureCurrentProfile_existing_returnsWithoutSavingOrPublishing() {
        when(repository.findById(SUB)).thenReturn(Optional.of(new CustomerProfile(SUB, "lan@example.com", "Lan")));

        CustomerProfileView view = service.ensureCurrentProfile();

        assertThat(view.fullName()).isEqualTo("Lan");
        verify(repository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void ensureCurrentProfile_jwtWithoutEmail_isBadRequest_andNothingSaved() {
        when(repository.findById(SUB)).thenReturn(Optional.empty());
        when(currentUser.email()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ensureCurrentProfile())
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ProfileErrorKeys.EMAIL_MISSING);
        verify(repository, never()).save(any());
    }

    @Test
    void updateCurrent_staleVersion_isConflict_andNothingSaved() {
        CustomerProfile profile = new CustomerProfile(SUB, "lan@example.com", "Lan");
        profile.setVersion(3);
        when(repository.findById(SUB)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateCurrent(new ProfileUpdateCommand("Lan Nguyễn", null, null, null, 2L)))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateCurrent_appliesFieldsAndTrims() {
        CustomerProfile profile = new CustomerProfile(SUB, "lan@example.com", "Lan");
        when(repository.findById(SUB)).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerProfileView view = service.updateCurrent(
                new ProfileUpdateCommand("  Lan Nguyễn ", "0912345678", LocalDate.of(1995, 5, 20), "FEMALE", 0L));

        assertThat(view.fullName()).isEqualTo("Lan Nguyễn");
        assertThat(view.phone()).isEqualTo("0912345678");
        assertThat(view.gender()).isEqualTo("FEMALE");
        assertThat(view.birthday()).isEqualTo(LocalDate.of(1995, 5, 20));
    }

    @Test
    void setStatus_unknownCustomer_isNotFound() {
        UUID other = UUID.randomUUID();
        when(repository.findById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setStatus(other, CustomerStatus.LOCKED)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }
}
