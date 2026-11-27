package com.donsang.notification.events;

import java.util.Map;
import java.util.UUID;

public record EmailNotificationEvent(
        String eventId,
        Type type,
        String recipientEmail,
        String recipientName,
        String reference,
        Map<String, String> data) {

    public EmailNotificationEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
        if (type == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        recipientName = recipientName == null ? "" : recipientName;
        reference = reference == null ? "" : reference;
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public enum Type {
        DONNEUR_INSCRIT,
        DON_STATUT_CHANGE,
        DEMANDE_STATUT_CHANGE,
        DONNEUR_SOLLICITE,
        STOCK_CRITIQUE
    }
}
