package vn.ecommerce.order.promotion.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.promotion.DiscountQuote;
import vn.ecommerce.order.promotion.VoucherView;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class PromotionServiceImpl implements PromotionService {

    static final String TARGET_VOUCHER = "VOUCHER";

    private final VoucherRepository vouchers;
    private final VoucherUsageRepository usages;
    private final PromotionMapper mapper;
    private final CurrentUser currentUser;
    private final AdminAuditRecorder audit;

    @Override
    @Transactional(readOnly = true)
    public DiscountQuote quote(String code, UUID customerId, BigDecimal subtotal) {
        Voucher voucher = vouchers.findByCodeIgnoreCase(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException(PromotionErrorKeys.NOT_FOUND));
        Instant now = Instant.now();
        if (voucher.getStatus() != Voucher.VoucherStatus.ACTIVE) {
            throw new BadRequestException(PromotionErrorKeys.INACTIVE);
        }
        if (now.isBefore(voucher.getStartsAt())) {
            throw new BadRequestException(PromotionErrorKeys.NOT_STARTED);
        }
        if (now.isAfter(voucher.getEndsAt())) {
            throw new BadRequestException(PromotionErrorKeys.EXPIRED);
        }
        if (voucher.isExhausted()) {
            throw new BadRequestException(PromotionErrorKeys.EXHAUSTED);
        }
        if (usages.countByVoucherIdAndCustomerId(voucher.getId(), customerId) >= voucher.getPerCustomerLimit()) {
            throw new BadRequestException(PromotionErrorKeys.ALREADY_USED);
        }
        if (subtotal.compareTo(voucher.getMinSubtotal()) < 0) {
            throw new BadRequestException(PromotionErrorKeys.MIN_SUBTOTAL, voucher.getMinSubtotal().toPlainString());
        }
        return new DiscountQuote(voucher.getId(), voucher.getCode(), voucher.discountFor(subtotal), voucher.getDescription());
    }

    @Override
    public void recordUsage(UUID voucherId, UUID orderId, UUID customerId, BigDecimal discount) {
        Voucher voucher = vouchers.findById(voucherId).orElseThrow(() -> new BadRequestException(PromotionErrorKeys.NOT_FOUND));
        if (voucher.isExhausted()) {
            throw new BadRequestException(PromotionErrorKeys.EXHAUSTED);
        }
        // @Version trên voucher: hai đơn cùng lấy lượt cuối thì một bên bị ObjectOptimisticLockingFailure → 409
        voucher.incrementUsed();
        vouchers.save(voucher);
        usages.save(new VoucherUsage(voucherId, orderId, customerId, discount));
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountQuote validateForCurrent(VoucherValidateCommand command) {
        return quote(command.code(), UUID.fromString(currentUser.requireId()), command.subtotal());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherView> search(String q, int page, int size) {
        String pattern = q == null || q.isBlank() ? null : "%" + q.trim().toUpperCase(Locale.ROOT) + "%";
        Page<Voucher> result = vouchers.search(pattern, PageRequest.of(page, size));
        return PageResponse.of(result.map(mapper::toView).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherView get(UUID id) {
        return mapper.toView(find(id));
    }

    @Override
    public VoucherView create(VoucherUpsertCommand command) {
        String code = command.code().trim().toUpperCase(Locale.ROOT);
        if (vouchers.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(PromotionErrorKeys.CODE_TAKEN, code);
        }
        Voucher voucher = new Voucher(code);
        apply(voucher, command);
        VoucherView view = mapper.toView(vouchers.save(voucher));
        audit.record("VOUCHER_CREATE", TARGET_VOUCHER, view.id().toString(), Map.of("code", code));
        return view;
    }

    @Override
    public VoucherView update(UUID id, VoucherUpsertCommand command) {
        if (command.version() == null) {
            throw new BadRequestException(CommonErrorKeys.VERSION_REQUIRED);
        }
        Voucher voucher = find(id);
        if (voucher.getVersion() != command.version()) {
            throw new ConflictException(CommonErrorKeys.CONCURRENT_MODIFICATION);
        }
        // Không đổi mã sau khi tạo: đơn cũ đã snapshot voucher_code
        apply(voucher, command);
        VoucherView view = mapper.toView(vouchers.save(voucher));
        audit.record("VOUCHER_UPDATE", TARGET_VOUCHER, id.toString(), Map.of("code", voucher.getCode()));
        return view;
    }

    @Override
    public void delete(UUID id) {
        Voucher voucher = find(id);
        voucher.markDeleted(currentUser.requireId());
        vouchers.save(voucher);
        audit.record("VOUCHER_DELETE", TARGET_VOUCHER, id.toString(), Map.of("code", voucher.getCode()));
    }

    private Voucher find(UUID id) {
        return vouchers.findById(id).orElseThrow(() -> new NotFoundException(PromotionErrorKeys.NOT_FOUND));
    }

    private static void apply(Voucher voucher, VoucherUpsertCommand command) {
        Voucher.VoucherType type = Voucher.VoucherType.valueOf(command.type());
        if (type == Voucher.VoucherType.PERCENT && (command.value().compareTo(BigDecimal.ONE) < 0 || command.value().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException(PromotionErrorKeys.PERCENT_RANGE);
        }
        if (!command.endsAt().isAfter(command.startsAt())) {
            throw new BadRequestException(PromotionErrorKeys.INVALID_PERIOD);
        }
        voucher.setType(type);
        voucher.setValue(command.value());
        voucher.setMaxDiscount(command.maxDiscount());
        voucher.setMinSubtotal(command.minSubtotal() == null ? BigDecimal.ZERO : command.minSubtotal());
        voucher.setStartsAt(command.startsAt());
        voucher.setEndsAt(command.endsAt());
        voucher.setUsageLimit(command.usageLimit());
        voucher.setPerCustomerLimit(command.perCustomerLimit() == null ? 1 : command.perCustomerLimit());
        voucher.setStatus(command.status() == null ? Voucher.VoucherStatus.ACTIVE : Voucher.VoucherStatus.valueOf(command.status()));
        voucher.setDescription(command.description());
    }
}
