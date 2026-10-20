package vn.ecommerce.customer.profile.internal;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.events.customer.CustomerCreatedEvent;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.customer.profile.CustomerProfileView;
import vn.ecommerce.customer.profile.CustomerStatus;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class ProfileServiceImpl implements ProfileService {

    private final CustomerProfileRepository repository;
    private final ProfileMapper mapper;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;

    @Override
    public CustomerProfileView ensureCurrentProfile() {
        UUID id = UUID.fromString(currentUser.requireId());
        return mapper.toView(repository.findById(id).orElseGet(() -> createFromJwt(id)));
    }

    @Override
    public CustomerProfileView updateCurrent(ProfileUpdateCommand command) {
        UUID id = UUID.fromString(currentUser.requireId());
        CustomerProfile profile = repository.findById(id).orElseGet(() -> createFromJwt(id));
        if (profile.getVersion() != command.version()) {
            throw new ConflictException(CommonErrorKeys.CONCURRENT_MODIFICATION);
        }
        profile.setFullName(command.fullName().trim());
        profile.setPhone(command.phone());
        profile.setBirthday(command.birthday());
        profile.setGender(command.gender() == null ? null : CustomerProfile.Gender.valueOf(command.gender()));
        return mapper.toView(repository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerProfileView> searchForAdmin(String q, int page, int size) {
        String pattern = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        Page<CustomerProfile> result = repository.search(pattern, PageRequest.of(page, size));
        return PageResponse.of(result.map(mapper::toView).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileView getForAdmin(UUID customerId) {
        return mapper.toView(repository.findById(customerId)
                .orElseThrow(() -> new NotFoundException(ProfileErrorKeys.NOT_FOUND)));
    }

    @Override
    public void setStatus(UUID customerId, CustomerStatus status) {
        CustomerProfile profile = repository.findById(customerId)
                .orElseThrow(() -> new NotFoundException(ProfileErrorKeys.NOT_FOUND));
        profile.setStatus(status);
        repository.save(profile);
    }

    /** Lần đầu khách gọi API sau khi đăng nhập: tạo hồ sơ từ claim, phát event trong cùng transaction (outbox). */
    private CustomerProfile createFromJwt(UUID id) {
        String email = currentUser.email().orElseThrow(() -> new BadRequestException(ProfileErrorKeys.EMAIL_MISSING));
        CustomerProfile profile = repository.save(new CustomerProfile(id, email, currentUser.displayName().orElse(null)));
        events.publishEvent(new CustomerCreatedEvent(UUID.randomUUID(), Instant.now(), id.toString(), email, profile.getFullName()));
        log.info("Tạo hồ sơ khách {} lần đầu đăng nhập", id);
        return profile;
    }
}
