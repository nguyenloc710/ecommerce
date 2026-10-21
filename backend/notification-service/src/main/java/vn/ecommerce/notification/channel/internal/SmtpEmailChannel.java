package vn.ecommerce.notification.channel.internal;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import vn.ecommerce.notification.NotificationProperties;
import vn.ecommerce.notification.channel.ChannelException;
import vn.ecommerce.notification.channel.ChannelType;
import vn.ecommerce.notification.channel.NotificationChannel;
import vn.ecommerce.notification.channel.OutboundMessage;

/** Email qua SMTP (Mailpit local, Brevo/Resend SMTP relay trên prod). */
@Slf4j
@Component
@RequiredArgsConstructor
class SmtpEmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public ChannelType type() {
        return ChannelType.EMAIL;
    }

    @Override
    public void send(OutboundMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(message.recipient());
            helper.setSubject(message.subject());
            helper.setText(message.body(), true);
            mailSender.send(mime);
            log.info("Đã gửi email '{}' tới {}", message.subject(), mask(message.recipient()));
        } catch (MessagingException | MailException e) {
            throw new ChannelException("Gửi email tới " + mask(message.recipient()) + " thất bại", e);
        }
    }

    /** Che email trong log: {@code l***@example.com}. */
    static String mask(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}
