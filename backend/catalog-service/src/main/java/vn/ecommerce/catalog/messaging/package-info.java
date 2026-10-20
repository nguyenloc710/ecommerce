/**
 * Module messaging: cấu hình RabbitMQ dùng chung (exchange, converter JSON) và sổ event đã xử lý
 * để consumer trong service idempotent.
 */
@ApplicationModule(displayName = "messaging")
package vn.ecommerce.catalog.messaging;

import org.springframework.modulith.ApplicationModule;
