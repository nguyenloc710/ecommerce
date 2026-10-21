package vn.ecommerce.notification.dispatch.internal;

import java.util.Map;
import java.util.UUID;
import vn.ecommerce.notification.channel.ChannelType;

/**
 * Một thông báo cần gửi, do listener dựng từ event.
 *
 * @param kind        nhãn nghiệp vụ ghi vào log (ORDER_CONFIRMED...)
 * @param template    tên template = tên file trong {@code templates/mail/} và key tiêu đề
 * @param subjectArgs tham số cho tiêu đề (ví dụ mã đơn)
 */
public record DispatchRequest(UUID eventId, String kind, ChannelType channel, String recipient, String template,
                              Map<String, Object> model, Object... subjectArgs) {
}
