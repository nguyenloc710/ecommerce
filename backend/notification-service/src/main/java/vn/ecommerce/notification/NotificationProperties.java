package vn.ecommerce.notification;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình chung của service (package gốc nên mọi module dùng được).
 *
 * @param shopName      tên shop hiển thị trong email
 * @param from          địa chỉ gửi, dạng {@code Tên <email>}
 * @param storefrontUrl gốc URL web để dựng link "Xem đơn hàng"
 * @param defaultLocale ngôn ngữ email khi event không mang locale
 */
@ConfigurationProperties(prefix = "ecommerce.notification")
public record NotificationProperties(String shopName, String from, String storefrontUrl, String defaultLocale) {

    public NotificationProperties {
        shopName = shopName == null || shopName.isBlank() ? "Shop" : shopName;
        defaultLocale = defaultLocale == null || defaultLocale.isBlank() ? "vi" : defaultLocale;
        storefrontUrl = storefrontUrl == null ? "" : storefrontUrl.replaceAll("/+$", "");
    }

    public Locale locale() {
        return Locale.forLanguageTag(defaultLocale);
    }
}
