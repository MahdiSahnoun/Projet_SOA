package com.donsang.notification;

import com.donsang.notification.events.EmailNotificationEvent;
import com.donsang.notification.history.NotificationAttempt;
import com.donsang.notification.history.DeliveryStatus;
import com.donsang.notification.history.NotificationHistoryRepository;
import com.donsang.notification.history.NotificationHistoryService;
import brave.Tracing;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "spring.datasource.url=jdbc:h2:mem:notification;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.history-api.username=test-reader",
        "app.history-api.password=test-password"
})
@AutoConfigureMockMvc
class NotificationApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NotificationHistoryRepository historyRepository;

	@Autowired
	private NotificationHistoryService historyService;

	@Autowired
	private Tracing braveTracing;

	@Test
	void contextLoads() {
	}

	@Test
	void braveTracingIsConfigured() {
	    assertNotNull(braveTracing);
	}

	@Test
	void historyApiRequiresBasicAuthentication() throws Exception {
	    mockMvc.perform(get("/api/notifications"))
	            .andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedCallerCanReadHistory() throws Exception {
	    String credentials = Base64.getEncoder().encodeToString(
	            "test-reader:test-password".getBytes(StandardCharsets.UTF_8));

	    mockMvc.perform(get("/api/notifications")
	                    .header("Authorization", "Basic " + credentials))
	            .andExpect(status().isOk());
	}

	@Test
	void retriesUpdateOneHistoryRowAndRetainTheFinalStatus() {
	    historyRepository.deleteAll();
	    EmailNotificationEvent event = new EmailNotificationEvent(
	            "event-persisted",
	            EmailNotificationEvent.Type.DONNEUR_INSCRIT,
	            "donneur@example.test",
	            "Amira",
	            "DON-42",
	            Map.of());
	    ConsumerRecord<String, String> record =
	            new ConsumerRecord<>("notifications.email", 0, 100L, null, "payload");

	    NotificationAttempt firstAttempt = historyService.beginAttempt(record, event);
	    historyService.markFailed(firstAttempt.historyId(), "MailSendException");
	    NotificationAttempt retry = historyService.beginAttempt(record, event);
	    historyService.markSent(retry.historyId());
	    NotificationAttempt duplicate = historyService.beginAttempt(record, event);

	    var history = historyRepository.findById(firstAttempt.historyId()).orElseThrow();
	    assertEquals(firstAttempt.historyId(), retry.historyId());
	    assertTrue(duplicate.alreadySent());
	    assertEquals(2, history.getAttemptCount());
	    assertEquals(DeliveryStatus.SENT, history.getStatus());
	    assertEquals("donneur@example.test", history.getRecipientEmail());
	}
}
