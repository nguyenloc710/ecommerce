package vn.ecommerce.customer.address.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.customer.address.AddressView;
import vn.ecommerce.customer.address.internal.division.AdminDivision;
import vn.ecommerce.customer.address.internal.division.DivisionService;
import vn.ecommerce.customer.profile.CustomerProfileView;
import vn.ecommerce.customer.profile.ProfileApi;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    private static final UUID SUB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock AddressRepository repository;
    @Mock DivisionService divisions;
    @Mock CurrentUser currentUser;
    @Mock ProfileApi profileApi;

    private AddressServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AddressServiceImpl(repository, divisions, Mappers.getMapper(AddressMapper.class),
                new AddressProperties(2), currentUser, profileApi);
    }

    @Test
    void create_firstAddress_becomesDefault_andSnapshotsDivisionNames() {
        when(profileApi.ensureCurrentProfile()).thenReturn(profile());
        when(repository.countByCustomerId(SUB)).thenReturn(0L);
        stubDivisions();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddressView view = service.create(command(false, null));

        assertThat(view.isDefault()).isTrue();
        assertThat(view.provinceName()).isEqualTo("Thành phố Hà Nội");
        assertThat(view.wardName()).isEqualTo("Phường Ba Đình");
        assertThat(view.recipientName()).isEqualTo("Lan Nguyễn");
        verify(repository).clearDefaultExcept(eq(SUB), any());
    }

    @Test
    void create_overLimit_isBadRequest_andNothingSaved() {
        when(profileApi.ensureCurrentProfile()).thenReturn(profile());
        when(repository.countByCustomerId(SUB)).thenReturn(2L);

        assertThatThrownBy(() -> service.create(command(false, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AddressErrorKeys.LIMIT_REACHED);
        verify(repository, never()).save(any());
    }

    @Test
    void create_wardOutsideProvince_isBadRequest() {
        when(profileApi.ensureCurrentProfile()).thenReturn(profile());
        when(repository.countByCustomerId(SUB)).thenReturn(0L);
        when(divisions.requireProvince("01")).thenReturn(division("01", "Thành phố Hà Nội", null));
        when(divisions.requireWard("01", "99999"))
                .thenThrow(new BadRequestException(AddressErrorKeys.WARD_NOT_IN_PROVINCE, "99999", "01"));

        assertThatThrownBy(() -> service.create(new AddressUpsertCommand("Lan", "0912345678", "01", "99999", "1 Hàng Bài", null, false, null)))
                .isInstanceOf(BadRequestException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_withoutVersion_isBadRequest() {
        assertThatThrownBy(() -> service.update(UUID.randomUUID(), command(false, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(CommonErrorKeys.VERSION_REQUIRED);
    }

    @Test
    void update_staleVersion_isConflict() {
        when(currentUser.requireId()).thenReturn(SUB.toString());
        Address existing = new Address(SUB);
        existing.setVersion(5);
        UUID id = existing.getId();
        when(repository.findByIdAndCustomerId(id, SUB)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(id, command(false, 4L))).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_keepsDefaultFlag_whenClientSendsFalse() {
        when(currentUser.requireId()).thenReturn(SUB.toString());
        Address existing = new Address(SUB);
        existing.setDefault(true);
        when(repository.findByIdAndCustomerId(existing.getId(), SUB)).thenReturn(Optional.of(existing));
        stubDivisions();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddressView view = service.update(existing.getId(), command(false, 0L));

        assertThat(view.isDefault()).isTrue();
    }

    @Test
    void delete_ofSomeoneElsesAddress_isNotFound() {
        when(currentUser.requireId()).thenReturn(SUB.toString());
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndCustomerId(id, SUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_softDeletes_andClearsDefault() {
        when(currentUser.requireId()).thenReturn(SUB.toString());
        Address existing = new Address(SUB);
        existing.setDefault(true);
        when(repository.findByIdAndCustomerId(existing.getId(), SUB)).thenReturn(Optional.of(existing));

        service.delete(existing.getId());

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedBy()).isEqualTo(SUB.toString());
        assertThat(existing.isDefault()).isFalse();
        verify(repository).save(existing);
    }

    private void stubDivisions() {
        when(divisions.requireProvince("01")).thenReturn(division("01", "Thành phố Hà Nội", null));
        when(divisions.requireWard("01", "00004")).thenReturn(division("00004", "Phường Ba Đình", "01"));
    }

    private static AddressUpsertCommand command(boolean isDefault, Long version) {
        return new AddressUpsertCommand(" Lan Nguyễn ", "0912345678", "01", "00004", "1 Hàng Bài", " ", isDefault, version);
    }

    private static CustomerProfileView profile() {
        return new CustomerProfileView(SUB, "lan@example.com", "Lan", null, null, null, "ACTIVE", Instant.now(), 0);
    }

    /** Entity tham chiếu không có constructor public: dựng qua reflection cho test. */
    private static AdminDivision division(String code, String name, String parentCode) {
        AdminDivision division = org.springframework.beans.BeanUtils.instantiateClass(AdminDivision.class);
        ReflectionTestUtils.setField(division, "code", code);
        ReflectionTestUtils.setField(division, "name", name);
        ReflectionTestUtils.setField(division, "parentCode", parentCode);
        return division;
    }
}
