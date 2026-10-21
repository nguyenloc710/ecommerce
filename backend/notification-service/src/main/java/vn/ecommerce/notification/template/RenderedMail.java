package vn.ecommerce.notification.template;

/** Email đã render: tiêu đề (từ messages) và thân HTML (từ Thymeleaf). */
public record RenderedMail(String subject, String html) {
}
