package vn.ecommerce.customer.address.internal;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.customer.address.AddressView;
import vn.ecommerce.customer.address.internal.division.AdminDivision;
import vn.ecommerce.customer.address.internal.division.DivisionService;
import vn.ecommerce.customer.profile.ProfileApi;

@Service
@RequiredArgsConstructor
@Transactional
class AddressServiceImpl implements AddressService {

    private final AddressRepository repository;
    private final DivisionService divisions;
    private final AddressMapper mapper;
    private final AddressProperties properties;
    private final CurrentUser currentUser;
    private final ProfileApi profileApi;

    @Override
    @Transactional(readOnly = true)
    public List<AddressView> listMine() {
        return repository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(currentId()).stream().map(mapper::toView).toList();
    }

    @Override
    public AddressView create(AddressUpsertCommand command) {
        // Đảm bảo hồ sơ tồn tại (FK) kể cả khi khách thêm địa chỉ trước khi mở trang tài khoản
        UUID customerId = profileApi.ensureCurrentProfile().id();
        long existing = repository.countByCustomerId(customerId);
        if (existing >= properties.maxPerCustomer()) {
            throw new BadRequestException(AddressErrorKeys.LIMIT_REACHED, properties.maxPerCustomer());
        }
        Address address = new Address(customerId);
        apply(address, command);
        // Địa chỉ đầu tiên luôn là mặc định
        address.setDefault(wantsDefault(command) || existing == 0);
        return saveRespectingDefault(address);
    }

    @Override
    public AddressView update(UUID addressId, AddressUpsertCommand command) {
        if (command.version() == null) {
            throw new BadRequestException(CommonErrorKeys.VERSION_REQUIRED);
        }
        Address address = ownedOrThrow(addressId);
        if (address.getVersion() != command.version()) {
            throw new ConflictException(CommonErrorKeys.CONCURRENT_MODIFICATION);
        }
        apply(address, command);
        // Không cho bỏ cờ mặc định của địa chỉ đang mặc định; muốn đổi thì đặt địa chỉ khác làm mặc định
        address.setDefault(address.isDefault() || wantsDefault(command));
        return saveRespectingDefault(address);
    }

    @Override
    public AddressView setDefault(UUID addressId) {
        Address address = ownedOrThrow(addressId);
        address.setDefault(true);
        return saveRespectingDefault(address);
    }

    @Override
    public void delete(UUID addressId) {
        Address address = ownedOrThrow(addressId);
        address.markDeleted(currentUser.requireId());
        address.setDefault(false);
        repository.save(address);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressView getForCustomer(UUID customerId, UUID addressId) {
        return repository.findByIdAndCustomerId(addressId, customerId).map(mapper::toView)
                .orElseThrow(() -> new NotFoundException(AddressErrorKeys.NOT_FOUND));
    }

    private Address ownedOrThrow(UUID addressId) {
        return repository.findByIdAndCustomerId(addressId, currentId())
                .orElseThrow(() -> new NotFoundException(AddressErrorKeys.NOT_FOUND));
    }

    private static boolean wantsDefault(AddressUpsertCommand command) {
        return Boolean.TRUE.equals(command.isDefault());
    }

    private UUID currentId() {
        return UUID.fromString(currentUser.requireId());
    }

    /** Kiểm tra phường thuộc tỉnh rồi chép dữ liệu, kèm snapshot tên đơn vị hành chính. */
    private void apply(Address address, AddressUpsertCommand command) {
        AdminDivision province = divisions.requireProvince(command.provinceCode());
        AdminDivision ward = divisions.requireWard(command.provinceCode(), command.wardCode());
        address.setRecipientName(command.recipientName().trim());
        address.setPhone(command.phone());
        address.setProvinceCode(province.getCode());
        address.setProvinceName(province.getName());
        address.setWardCode(ward.getCode());
        address.setWardName(ward.getName());
        address.setLine1(command.line1().trim());
        address.setNote(command.note() == null || command.note().isBlank() ? null : command.note().trim());
    }

    /** Lưu; nếu là mặc định thì bỏ cờ của các địa chỉ khác trước để không vi phạm unique partial index. */
    private AddressView saveRespectingDefault(Address address) {
        if (address.isDefault()) {
            repository.clearDefaultExcept(address.getCustomerId(), address.getId());
        }
        return mapper.toView(repository.save(address));
    }
}
