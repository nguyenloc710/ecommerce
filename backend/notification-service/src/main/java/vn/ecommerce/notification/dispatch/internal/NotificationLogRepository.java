package vn.ecommerce.notification.dispatch.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ecommerce.notification.channel.ChannelType;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsByEventIdAndChannel(UUID eventId, ChannelType channel);
}
