package com.donsang.notification.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, UUID> {

    Optional<NotificationHistory> findByKafkaTopicAndKafkaPartitionAndKafkaOffset(
            String topic, int partition, long offset);

    Page<NotificationHistory> findAllByOrderByReceivedAtDesc(Pageable pageable);

    long deleteByReceivedAtBefore(Instant cutoff);
}
