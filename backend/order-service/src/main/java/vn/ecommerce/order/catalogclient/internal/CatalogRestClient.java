package vn.ecommerce.order.catalogclient.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.ErrorResponse;
import vn.ecommerce.order.catalogclient.CatalogClient;
import vn.ecommerce.order.catalogclient.CatalogUnavailableException;
import vn.ecommerce.order.catalogclient.ReservationOutcome;
import vn.ecommerce.order.catalogclient.ReserveLine;
import vn.ecommerce.order.catalogclient.SkuSnapshot;

@Slf4j
@Component
class CatalogRestClient implements CatalogClient {

    private static final ParameterizedTypeReference<List<SkuSnapshot>> SKU_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;

    CatalogRestClient(RestClient catalogHttpClient) {
        this.client = catalogHttpClient;
    }

    @Override
    public List<SkuSnapshot> lookup(List<UUID> skuIds) {
        if (skuIds.isEmpty()) {
            return List.of();
        }
        try {
            List<SkuSnapshot> result = client.post().uri("/internal/skus/lookup").body(Map.of("skuIds", skuIds))
                    .retrieve().body(SKU_LIST);
            return result == null ? List.of() : result;
        } catch (HttpClientErrorException e) {
            throw businessError(e);
        } catch (RestClientException e) {
            throw unavailable("lookup", e);
        }
    }

    @Override
    public ReservationOutcome reserve(UUID orderId, List<ReserveLine> lines) {
        try {
            ReservationOutcome outcome = client.post().uri("/internal/reservations")
                    .body(Map.of("orderId", orderId, "lines", lines)).retrieve().body(ReservationOutcome.class);
            if (outcome == null) {
                throw new CatalogUnavailableException();
            }
            return outcome;
        } catch (HttpClientErrorException e) {
            throw businessError(e);
        } catch (RestClientException e) {
            throw unavailable("reserve", e);
        }
    }

    @Override
    public void release(UUID orderId) {
        try {
            client.post().uri("/internal/reservations/{orderId}/release", orderId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            // Không ném: reservation tự hết hạn theo TTL (B-03), chỉ ghi log để theo dõi
            log.warn("Không nhả được hàng cho đơn {} ({}); sẽ tự hết hạn", orderId, e.getMessage());
        }
    }

    /** 4xx của catalog mang {@code ErrorResponse}; ném lại cùng message key để khách thấy đúng lý do (hết hàng...). */
    private RuntimeException businessError(HttpClientErrorException e) {
        ErrorResponse body = null;
        try {
            body = e.getResponseBodyAs(ErrorResponse.class);
        } catch (RuntimeException ignored) {
            // body không phải ErrorResponse: coi như catalog lỗi
        }
        if (body == null || body.code() == null) {
            return unavailable("4xx không có ErrorResponse", e);
        }
        return e.getStatusCode().value() == 409 ? new ConflictException(body.code()) : new BadRequestException(body.code());
    }

    private CatalogUnavailableException unavailable(String operation, RestClientException cause) {
        log.error("catalog-service lỗi ở {}: {}", operation, cause.getMessage());
        return new CatalogUnavailableException();
    }
}
