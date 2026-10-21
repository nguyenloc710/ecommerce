package vn.ecommerce.notification.template;

import java.util.Locale;
import java.util.Map;

/** API public của module template. Tiêu đề lấy từ key {@code mail.<template>.subject}; model luôn có thêm {@code shopName}, {@code storefrontUrl}. */
public interface TemplateRenderer {

    RenderedMail render(String templateName, Locale locale, Map<String, Object> model, Object... subjectArgs);
}
