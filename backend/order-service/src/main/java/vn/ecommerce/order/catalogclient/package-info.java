/**
 * Module catalogclient: Adapter gọi {@code /internal/**} của catalog-service (tra SKU, giữ/nhả hàng) bằng token client credentials.
 * Nghiệp vụ (cart, checkout, orders) chỉ phụ thuộc {@link vn.ecommerce.order.catalogclient.CatalogClient}.
 */
@ApplicationModule(displayName = "catalogclient")
package vn.ecommerce.order.catalogclient;

import org.springframework.modulith.ApplicationModule;
