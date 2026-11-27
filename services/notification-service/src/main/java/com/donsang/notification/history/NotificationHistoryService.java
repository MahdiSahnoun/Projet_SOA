package com.donsang.notification.history;

import com.donsang.notification.events.EmailNotificationEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationHistoryService {

    private final NotificationHistoryRepository repository;

    public NotificationHistoryService(NotificationHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationAttempt beginAttempt(ConsumerRecord<String, String> record, EmailNotificationEvent event) {
        Optional<NotificationHistory> existing = repository
                .findByKafkaTopicAndKafkaPartitionAndKafkaOffset(
                        record.topic(), record.partition(), record.offset());
        if (existing.isPresent() && existing.get().getStatus() == DeliveryStatus.SENT) {
            return new NotificationAttempt(existing.get().getId(), true);
        }
        NotificationHistory history = existing.orElseGet(
                () -> new NotificationHistory(record.topic(), record.partition(), record.offset()));
        history.beginAttempt(event);
        return new NotificationAttempt(repository.save(history).getId(), false);
    }

    @Transactional
    public void markSent(UUID id) {
        NotificationHistory history = repository.findById(id).orElseThrow();
        history.markSent();
    }

    @Transactional
    public void markFailed(UUID id, String errorSummary) {
        NotificationHistory history = repository.findById(id).orElseThrow();
        history.markFailed(errorSummary);
    }

    @Transactional
    public long deleteExpired(java.time.Instant cutoff) {
        return repository.deleteByReceivedAtBefore(cutoff);
    }
}
