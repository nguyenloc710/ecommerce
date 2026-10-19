package vn.ecommerce.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import vn.ecommerce.common.web.error.GlobalExceptionHandler;
import vn.ecommerce.common.web.trace.TraceIdResponseFilter;

/** Tự đăng ký handler lỗi và filter trace cho mọi service servlet dùng common-web. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

    @Bean
    GlobalExceptionHandler globalExceptionHandler(MessageSource messageSource) {
        return new GlobalExceptionHandler(messageSource);
    }

    @Bean
    FilterRegistrationBean<TraceIdResponseFilter> traceIdResponseFilter() {
        FilterRegistrationBean<TraceIdResponseFilter> registration =
                new FilterRegistrationBean<>(new TraceIdResponseFilter());
        // Sau filter observation của Spring (HIGHEST_PRECEDENCE + 1) để MDC đã có traceId.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
