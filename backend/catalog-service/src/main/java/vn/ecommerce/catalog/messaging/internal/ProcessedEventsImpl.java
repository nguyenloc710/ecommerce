package vn.ecommerce.catalog.messaging.internal;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.messaging.ProcessedEvents;

@Service
@RequiredArgsConstructor
class ProcessedEventsImpl implements ProcessedEvents {

    private final ProcessedEventRepository repository;

    /** MANDATORY: buộc consumer gọi trong transaction của mình để dấu "đã xử lý" rollback cùng nghiệp vụ. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(UUID eventId, String consumer) {
        ProcessedEvent.Key key = new ProcessedEvent.Key(eventId, consumer);
        if (repository.existsById(key)) {
            return false;
        }
        repository.saveAndFlush(new ProcessedEvent(eventId, consumer));
        return true;
    }
}
