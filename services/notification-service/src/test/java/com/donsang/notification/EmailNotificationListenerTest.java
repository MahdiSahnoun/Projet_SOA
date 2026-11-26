package com.donsang.notification;

import com.donsang.notification.events.EmailNotificationEvent;
import com.donsang.notification.history.NotificationAttempt;
import com.donsang.notification.history.NotificationHistoryService;
import com.donsang.notification.service.EmailNotificationListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailNotificationListenerTest {

    @Test
    void envoieUnEmailDeSollicitationAuxDonneurs() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationHistoryService historyService = mock(NotificationHistoryService.class);
        UUID historyId = UUID.randomUUID();
        EmailNotificationListener listener =
                new EmailNotificationListener(new ObjectMapper(), mailSender, "no-reply@donsang.local", historyService);
        EmailNotificationEvent event = new EmailNotificationEvent(
                "event-1",
                EmailNotificationEvent.Type.DONNEUR_SOLLICITE,
                "donneur@example.test",
                "Amira",
                "DEM-42",
                Map.of("groupeSanguin", "O_NEGATIF", "quantite", "2"));
        ConsumerRecord<String, String> record = record(new ObjectMapper().writeValueAsString(event));
        when(historyService.beginAttempt(any(), any())).thenReturn(new NotificationAttempt(historyId, false));

        listener.recevoir(record);

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("donneur@example.test", captor.getValue().getTo()[0]);
        org.junit.jupiter.api.Assertions.assertEquals("Appel aux donneurs", captor.getValue().getSubject());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getText().contains("DEM-42"));
        verify(historyService).markSent(historyId);
    }

    @Test
    void messageKafkaInvalideEchoueSansEnvoyerEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationHistoryService historyService = mock(NotificationHistoryService.class);
        UUID historyId = UUID.randomUUID();
        EmailNotificationListener listener =
                new EmailNotificationListener(new ObjectMapper(), mailSender, "no-reply@donsang.local", historyService);
        when(historyService.beginAttempt(any(), any())).thenReturn(new NotificationAttempt(historyId, false));

        assertThrows(IllegalArgumentException.class, () -> listener.recevoir(record("{invalid-json")));
        verifyNoInteractions(mailSender);
        verify(historyService).markFailed(historyId, "Invalid notification event");
    }

    @Test
    void persisteEchecEtPropagateErreurPourKafkaRetry() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationHistoryService historyService = mock(NotificationHistoryService.class);
        UUID historyId = UUID.randomUUID();
        EmailNotificationListener listener =
                new EmailNotificationListener(new ObjectMapper(), mailSender, "no-reply@donsang.local", historyService);
        EmailNotificationEvent event = new EmailNotificationEvent(
                "event-failed",
                EmailNotificationEvent.Type.DONNEUR_INSCRIT,
                "donneur@example.test",
                "Amira",
                "",
                Map.of());
        when(historyService.beginAttempt(any(), any())).thenReturn(new NotificationAttempt(historyId, false));
        doThrow(new MailSendException("mail unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(MailSendException.class,
                () -> listener.recevoir(record(new ObjectMapper().writeValueAsString(event))));

        verify(historyService).markFailed(historyId, "MailSendException");
    }

    @Test
    void doesNotResendAlreadyDeliveredKafkaRecord() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationHistoryService historyService = mock(NotificationHistoryService.class);
        UUID historyId = UUID.randomUUID();
        EmailNotificationListener listener =
                new EmailNotificationListener(new ObjectMapper(), mailSender, "no-reply@donsang.local", historyService);
        EmailNotificationEvent event = new EmailNotificationEvent(
                "event-already-sent",
                EmailNotificationEvent.Type.DONNEUR_INSCRIT,
                "donneur@example.test",
                "Amira",
                "",
                Map.of());
        when(historyService.beginAttempt(any(), any())).thenReturn(new NotificationAttempt(historyId, true));

        listener.recevoir(record(new ObjectMapper().writeValueAsString(event)));

        verifyNoInteractions(mailSender);
    }

    private static ConsumerRecord<String, String> record(String payload) {
        return new ConsumerRecord<>("notifications.email", 0, 42L, null, payload);
    }
}
