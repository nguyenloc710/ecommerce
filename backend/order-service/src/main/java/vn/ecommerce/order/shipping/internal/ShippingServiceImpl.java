package vn.ecommerce.order.shipping.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.ForbiddenException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.common.web.security.HmacSignatures;
import vn.ecommerce.order.shipping.ShipmentStatusChanged;
import vn.ecommerce.order.shipping.ShipmentView;
import vn.ecommerce.order.shipping.ShippingOption;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class ShippingServiceImpl implements ShippingService {

    private final CarrierRegistry registry;
    private final ShipmentRepository shipments;
    private final ShippingProperties properties;
    private final ObjectMapper json;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<ShippingOption> options(BigDecimal subtotal, String provinceCode) {
        return registry.all().stream().map(c -> registry.optionFor(c, subtotal, provinceCode)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingOption option(String carrierCode, BigDecimal subtotal, String provinceCode) {
        return registry.optionFor(carrier(carrierCode), subtotal, provinceCode);
    }

    @Override
    public ShipmentView createShipment(UUID orderId, String carrierCode, BigDecimal fee) {
        if (shipments.findByOrderId(orderId).isPresent()) {
            throw new ConflictException(ShippingErrorKeys.SHIPMENT_EXISTS);
        }
        ShippingCarrier carrier = carrier(carrierCode);
        Shipment shipment = shipments.save(new Shipment(orderId, carrier.code(), carrier.createTracking(orderId), fee));
        log.info("Tạo vận đơn {} ({}) cho đơn {}", shipment.getTrackingCode(), carrier.code(), orderId);
        return toView(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShipmentView> findByOrder(UUID orderId) {
        return shipments.findByOrderId(orderId).map(ShippingServiceImpl::toView);
    }

    @Override
    public void handleMockWebhook(String signature, String rawBody) {
        if (!HmacSignatures.matches(properties.mock().secret(), rawBody, signature)) {
            log.warn("Webhook vận chuyển giả có chữ ký sai");
            throw new ForbiddenException(ShippingErrorKeys.INVALID_SIGNATURE);
        }
        MockPayload payload = json.readValue(rawBody, MockPayload.class);
        Shipment shipment = shipments.findByCarrierAndTrackingCode(carrierOfTracking(payload.trackingCode()), payload.trackingCode())
                .orElseThrow(() -> new NotFoundException(ShippingErrorKeys.TRACKING_NOT_FOUND, payload.trackingCode()));
        Shipment.ShipmentStatus status = Shipment.ShipmentStatus.valueOf(payload.status());
        if (shipment.getStatus() == status) {
            return; // webhook gửi lại: bỏ qua
        }
        shipment.setStatus(status);
        shipment.setRaw(Map.of("trackingCode", payload.trackingCode(), "status", payload.status()));
        shipments.save(shipment);
        events.publishEvent(new ShipmentStatusChanged(shipment.getOrderId(), shipment.getCarrier(), shipment.getTrackingCode(), status.name()));
    }

    private ShippingCarrier carrier(String code) {
        return registry.find(code).orElseThrow(() -> new BadRequestException(ShippingErrorKeys.CARRIER_NOT_FOUND, code));
    }

    /** Mã giả dạng {@code MOCK-<CARRIER>-xxxx}. */
    private static String carrierOfTracking(String trackingCode) {
        String[] parts = trackingCode == null ? new String[0] : trackingCode.split("-");
        return parts.length >= 3 ? parts[1] : "";
    }

    private static ShipmentView toView(Shipment s) {
        return new ShipmentView(s.getId(), s.getOrderId(), s.getCarrier(), s.getTrackingCode(), s.getFee(), s.getStatus().name());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MockPayload(String trackingCode, String status) {
    }
}
