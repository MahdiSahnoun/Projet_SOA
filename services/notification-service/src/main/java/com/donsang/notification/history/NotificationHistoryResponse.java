package com.donsang.notification.history;

import java.time.Instant;
import java.util.UUID;

public record NotificationHistoryResponse(
        UUID id,
        String eventId,
        String type,
        String recipientEmail,
        String reference,
        DeliveryStatus status,
        int attemptCount,
        Instant receivedAt,
        Instant completedAt,
        String errorSummary) {

    static NotificationHistoryResponse from(NotificationHistory history) {
        return new NotificationHistoryResponse(
                history.getId(),
                history.getEventId(),
                history.getType(),
                history.getRecipientEmail(),
                history.getReference(),
                history.getStatus(),
                history.getAttemptCount(),
                history.getReceivedAt(),
                history.getCompletedAt(),
                history.getErrorSummary());
    }
}
