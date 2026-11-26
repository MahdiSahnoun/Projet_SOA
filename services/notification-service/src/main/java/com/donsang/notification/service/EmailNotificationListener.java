package com.donsang.notification.service;

import com.donsang.notification.events.EmailNotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailNotificationListener {

    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;
    private final String senderAddress;

    public EmailNotificationListener(ObjectMapper objectMapper, JavaMailSender mailSender,
                                     @Value("${app.mail.from}") String senderAddress) {
        this.objectMapper = objectMapper;
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }

    @KafkaListener(topics = "${app.kafka.email-topic}")
    public void recevoir(String payload) {
        EmailNotificationEvent event;
        try {
            event = objectMapper.readValue(payload, EmailNotificationEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid email notification event", e);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(event.recipientEmail());
        message.setSubject(sujet(event));
        message.setText(corps(event));
        mailSender.send(message);
    }

    private static String sujet(EmailNotificationEvent event) {
        return switch (event.type()) {
            case DONNEUR_INSCRIT -> "Bienvenue sur Don de sang";
            case DON_STATUT_CHANGE -> "Mise à jour de votre don";
            case DEMANDE_STATUT_CHANGE -> "Mise à jour de votre demande de sang";
            case DONNEUR_SOLLICITE -> "Appel aux donneurs";
            case STOCK_CRITIQUE -> "Alerte : stock de sang critique";
        };
    }

    private static String corps(EmailNotificationEvent event) {
        String greeting = event.recipientName().isBlank()
                ? "Bonjour,"
                : "Bonjour " + event.recipientName() + ",";
        Map<String, String> data = event.data();
        String reference = event.reference().isBlank() ? "" : "\nRéférence : " + event.reference();

        return switch (event.type()) {
            case DONNEUR_INSCRIT -> greeting
                    + "\n\nVotre inscription à la plateforme de dons de sang est confirmée.";
            case DON_STATUT_CHANGE -> greeting
                    + "\n\nLe statut de votre proposition de don est : " + value(data, "statut") + "."
                    + reference;
            case DEMANDE_STATUT_CHANGE -> greeting
                    + "\n\nLe statut de votre demande est : " + value(data, "statut") + "."
                    + "\nGroupe sanguin : " + value(data, "groupeSanguin")
                    + "\nQuantité : " + value(data, "quantite")
                    + reference;
            case DONNEUR_SOLLICITE -> greeting
                    + "\n\nUne demande urgente nécessite des donneurs compatibles."
                    + "\nGroupe demandé : " + value(data, "groupeSanguin")
                    + "\nQuantité : " + value(data, "quantite")
                    + "\nSi vous souhaitez aider, connectez-vous à la plateforme ou contactez votre banque de sang."
                    + reference;
            case STOCK_CRITIQUE -> greeting
                    + "\n\nLe stock est passé sous le seuil critique."
                    + "\nBanque : " + value(data, "codeBanque")
                    + "\nGroupe sanguin : " + value(data, "groupeSanguin")
                    + "\nPoches disponibles : " + value(data, "disponibles")
                    + "\nSeuil critique : " + value(data, "seuil");
        };
    }

    private static String value(Map<String, String> data, String key) {
        return data.getOrDefault(key, "non précisé");
    }
}
