package com.donsang.notification.history;

import com.donsang.notification.events.EmailNotificationEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_notification_history_kafka_record",
                columnNames = {"kafka_topic", "kafka_partition", "kafka_offset"}),
        indexes = @Index(name = "idx_notification_history_received_at", columnList = "received_at"))
public class NotificationHistory {

    @Id
    private UUID id;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(name = "notification_type", length = 40)
    private String type;

    @Column(name = "recipient_email", length = 320)
    private String recipientEmail;

    @Column(name = "reference_value", length = 512)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 16)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_summary", length = 256)
    private String errorSummary;

    @Column(name = "kafka_topic", nullable = false, length = 249)
    private String kafkaTopic;

    @Column(name = "kafka_partition", nullable = false)
    private int kafkaPartition;

    @Column(name = "kafka_offset", nullable = false)
    private long kafkaOffset;

    protected NotificationHistory() {
    }

    public NotificationHistory(String topic, int partition, long offset) {
        this.id = UUID.randomUUID();
        this.kafkaTopic = topic;
        this.kafkaPartition = partition;
        this.kafkaOffset = offset;
    }

    public void beginAttempt(EmailNotificationEvent event) {
        Instant now = Instant.now();
        if (receivedAt == null) {
            receivedAt = now;
        }
        attemptCount++;
        if (event != null) {
            eventId = event.eventId();
            type = event.type().name();
            recipientEmail = event.recipientEmail();
            reference = event.reference();
        }
        status = DeliveryStatus.PROCESSING;
        completedAt = null;
        errorSummary = null;
    }

    public void markSent() {
        status = DeliveryStatus.SENT;
        completedAt = Instant.now();
        errorSummary = null;
    }

    public void markFailed(String errorSummary) {
        status = DeliveryStatus.FAILED;
        completedAt = Instant.now();
        this.errorSummary = errorSummary;
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getReference() {
        return reference;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorSummary() {
        return errorSummary;
    }
}
