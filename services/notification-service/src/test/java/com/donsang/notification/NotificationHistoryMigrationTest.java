package com.donsang.notification;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationHistoryMigrationTest {

    @Test
    void createsNotificationHistoryTableInPostgresCompatibleMode() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:notification-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V1__create_notification_history.sql"));
            try (var result = connection.createStatement().executeQuery(
                    "SELECT delivery_status, kafka_offset FROM notification_history")) {
                assertEquals(2, result.getMetaData().getColumnCount());
            }
        }
    }
}
