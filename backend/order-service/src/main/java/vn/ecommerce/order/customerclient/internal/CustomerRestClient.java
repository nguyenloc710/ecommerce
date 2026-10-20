package vn.ecommerce.order.customerclient.internal;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.ecommerce.order.customerclient.CustomerAddress;
import vn.ecommerce.order.customerclient.CustomerClient;
import vn.ecommerce.order.customerclient.CustomerUnavailableException;

@Slf4j
@Component
class CustomerRestClient implements CustomerClient {

    private final RestClient client;

    CustomerRestClient(RestClient customerHttpClient) {
        this.client = customerHttpClient;
    }

    @Override
    public Optional<CustomerAddress> findAddress(UUID customerId, UUID addressId) {
        try {
            return Optional.ofNullable(client.get().uri("/internal/customers/{c}/addresses/{a}", customerId, addressId)
                    .retrieve().body(CustomerAddress.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("customer-service lỗi ở findAddress: {}", e.getMessage());
            throw new CustomerUnavailableException();
        }
    }
}
