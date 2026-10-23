package vn.ecommerce.catalog.product.internal.gold;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.internal.domain.GoldKarat;
import vn.ecommerce.catalog.product.internal.domain.GoldRate;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.GoldRateView;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.SetRateCommand;
import vn.ecommerce.catalog.product.internal.gold.GoldRateDtos.SetRateResult;
import vn.ecommerce.catalog.product.internal.repo.ProductVariantRepository;
import vn.ecommerce.common.jpa.audit.AdminAuditRecorder;
import vn.ecommerce.common.web.error.BadRequestException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class GoldRateServiceImpl implements GoldRateService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String AUDIT_ACTION = "GOLD_RATE_SET";
    private static final String AUDIT_TARGET = "GOLD_RATE";

    private final GoldRateRepository rates;
    private final ProductVariantRepository variants;
    private final GoldPricing pricing;
    private final AdminAuditRecorder audit;

    @Override
    @Transactional(readOnly = true)
    public List<GoldRateView> current() {
        return rates.findCurrent().stream().map(GoldRateServiceImpl::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageView<GoldRateView> history(String karat, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<GoldRate> found = karat == null || karat.isBlank() ? rates.findAllByOrderByEffectiveAtDesc(pageable)
                : rates.findByKaratOrderByEffectiveAtDesc(parseKarat(karat), pageable);
        return new PageView<>(found.getContent().stream().map(GoldRateServiceImpl::toView).toList(), found.getNumber(), found.getSize(),
                found.getTotalElements(), found.hasNext());
    }

    @Override
    public SetRateResult setRate(SetRateCommand command) {
        GoldKarat karat = parseKarat(command.karat());
        String note = command.note() == null || command.note().isBlank() ? null : command.note().trim();
        GoldRate rate = rates.save(new GoldRate(karat, command.pricePerGram(), Instant.now(), note));
        List<ProductVariant> repriced = variants.findGoldPricedByKarat(karat);
        repriced.forEach(v -> v.setPrice(pricing.price(v.getGoldWeight(), rate.getPricePerGram(), v.getMakingFee())));
        variants.saveAll(repriced);
        audit.record(AUDIT_ACTION, AUDIT_TARGET, karat.name(),
                Map.of("pricePerGram", rate.getPricePerGram().toPlainString(), "repricedVariants", repriced.size()));
        log.info("Giá vàng {} = {} đ/g, tính lại {} variant", karat, rate.getPricePerGram().toPlainString(), repriced.size());
        return new SetRateResult(toView(rate), repriced.size());
    }

    private static GoldKarat parseKarat(String karat) {
        try {
            return GoldKarat.valueOf(karat.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("validation.enum");
        }
    }

    private static GoldRateView toView(GoldRate r) {
        return new GoldRateView(r.getId(), r.getKarat().name(), r.getPricePerGram(), r.getEffectiveAt(), r.getNote(), r.getCreatedBy());
    }
}
