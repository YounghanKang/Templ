package com.example.demo.integration.slack;

import java.util.List;
import java.util.UUID;

public record SlackMessageBatch(
        String projectId,
        String channelId,
        String channelName,
        List<SlackMessage> messages
) {

    public SlackMessageBatch {

        messages =
                messages == null
                        ? List.of()
                        : List.copyOf(
                        messages
                );
    }
}