package vn.ecommerce.order.catalogclient.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param baseUrl gốc catalog-service (K8s: {@code http://catalog-service:8080}) */
@ConfigurationProperties(prefix = "ecommerce.clients.catalog")
public record CatalogClientProperties(String baseUrl) {
}
