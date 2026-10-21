package vn.ecommerce.notification.template.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.ecommerce.notification.NotificationProperties;
import vn.ecommerce.notification.template.RenderedMail;
import vn.ecommerce.notification.template.TemplateRenderer;

/** Template HTML nằm ở {@code classpath:/templates/mail/<tên>.html}. */
@Component
@RequiredArgsConstructor
class ThymeleafTemplateRenderer implements TemplateRenderer {

    static final String TEMPLATE_DIR = "mail/";
    static final String SUBJECT_KEY_PREFIX = "mail.";
    static final String SUBJECT_KEY_SUFFIX = ".subject";

    private final TemplateEngine templateEngine;
    private final MessageSource messages;
    private final NotificationProperties properties;

    @Override
    public RenderedMail render(String templateName, Locale locale, Map<String, Object> model, Object... subjectArgs) {
        Map<String, Object> variables = new HashMap<>(model);
        variables.put("shopName", properties.shopName());
        variables.put("storefrontUrl", properties.storefrontUrl());
        String subject = messages.getMessage(SUBJECT_KEY_PREFIX + templateName + SUBJECT_KEY_SUFFIX, subjectArgs, locale);
        String html = templateEngine.process(TEMPLATE_DIR + templateName, new Context(locale, variables));
        return new RenderedMail(subject, html);
    }
}
