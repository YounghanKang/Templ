package com.example.demo.integration.slack;

import java.time.Instant;
import java.util.UUID;

public record SlackMessage(
        UUID projectId,
        String eventId,
        String channelId,
        String channelName,
        String messageTs,
        String userId,
        String userName,
        String text,
        boolean generatedBySystem,
        Instant occurredAt
) {
}