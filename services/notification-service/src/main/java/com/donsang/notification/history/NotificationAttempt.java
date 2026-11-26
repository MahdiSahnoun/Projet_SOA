package com.donsang.notification.history;

import java.util.UUID;

public record NotificationAttempt(UUID historyId, boolean alreadySent) {
}
