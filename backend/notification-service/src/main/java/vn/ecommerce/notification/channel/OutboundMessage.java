package vn.ecommerce.notification.channel;

/** @param body HTML với email; văn bản thuần với SMS/Zalo */
public record OutboundMessage(String recipient, String subject, String body) {
}
