package com.donsang.notification;

import com.donsang.notification.events.EmailNotificationEvent;
import com.donsang.notification.service.EmailNotificationListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailNotificationListenerTest {

    @Test
    void envoieUnEmailDeSollicitationAuxDonneurs() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationListener listener =
                new EmailNotificationListener(new ObjectMapper(), mailSender, "no-reply@donsang.local");
        EmailNotificationEvent event = new EmailNotificationEvent(
                "event-1",
                EmailNotificationEvent.Type.DONNEUR_SOLLICITE,
                "donneur@example.test",
                "Amira",
                "DEM-42",
                Map.of("groupeSanguin", "O_NEGATIF", "quantite", "2"));

        listener.recevoir(new ObjectMapper().writeValueAsString(event));

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("donneur@example.test", captor.getValue().getTo()[0]);
        org.junit.jupiter.api.Assertions.assertEquals("Appel aux donneurs", captor.getValue().getSubject());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getText().contains("DEM-42"));
    }

    @Test
    void messageKafkaInvalideEchoueSansEnvoyerEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationListener listener =
                new EmailNotificationListener(new ObjectMapper(), mailSender, "no-reply@donsang.local");

        assertThrows(IllegalArgumentException.class, () -> listener.recevoir("{invalid-json"));
        verifyNoInteractions(mailSender);
    }
}
