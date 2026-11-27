package com.donsang.dons.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EmailNotificationPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationPublisher.class);

    private final KafkaTemplate<String, EmailNotificationEvent> kafkaTemplate;
    private final String topic;

    public EmailNotificationPublisher(KafkaTemplate<String, EmailNotificationEvent> kafkaTemplate,
                                      @Value("${app.kafka.email-topic:notifications.email}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publierApresCommit(EmailNotificationEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publier(event);
                }
            });
        } else {
            publier(event);
        }
    }

    private void publier(EmailNotificationEvent event) {
        kafkaTemplate.send(topic, event.eventId(), event)
                .whenComplete((resultat, erreur) -> {
                    if (erreur != null) {
                        LOGGER.error("Échec de publication de l'événement email {} ({})",
                                event.eventId(), event.type(), erreur);
                    }
                });
    }
}
