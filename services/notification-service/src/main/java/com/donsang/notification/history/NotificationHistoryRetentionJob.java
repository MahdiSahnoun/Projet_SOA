package com.donsang.notification.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class NotificationHistoryRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationHistoryRetentionJob.class);

    private final NotificationHistoryService historyService;
    private final long retentionDays;

    public NotificationHistoryRetentionJob(
            NotificationHistoryService historyService,
            @Value("${app.notification-history.retention-days:30}") long retentionDays) {
        this.historyService = historyService;
        this.retentionDays = retentionDays;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void purgeOnStartup() {
        purgeExpiredHistory();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredHistory() {
        long deleted = historyService.deleteExpired(Instant.now().minus(retentionDays, ChronoUnit.DAYS));
        if (deleted > 0) {
            log.info("Purged {} notification history records older than {} days", deleted, retentionDays);
        }
    }
}
