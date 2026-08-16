package com.example.demo.integration.slack;

import com.example.demo.collaboration.CollaborationEventIngestionService;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SlackBatchIngestionServiceTest {

    @Test
    void slackBatchIsConvertedAndIngested() {

        CollaborationEventIngestionService ingestionService =
                mock(
                        CollaborationEventIngestionService.class
                );


        SlackBatchIngestionService service =
                new SlackBatchIngestionService(
                        ingestionService
                );


        UUID projectId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );


        SlackMessage firstMessage =
                new SlackMessage(
                        projectId,
                        "event-001",
                        "channel-a",
                        "general",
                        "1000.001",
                        "user-001",
                        "alice",
                        "API 명세를 변경했습니다.",
                        false,
                        Instant.parse(
                                "2026-08-16T06:00:00Z"
                        )
                );


        SlackMessage secondMessage =
                new SlackMessage(
                        projectId,
                        "event-002",
                        "channel-a",
                        "general",
                        "1000.002",
                        "user-002",
                        "bob",
                        "백엔드 구현 방향을 수정했습니다.",
                        false,
                        Instant.parse(
                                "2026-08-16T06:01:00Z"
                        )
                );


        SlackMessageBatch batch =
                new SlackMessageBatch(
                        projectId,
                        "channel-a",
                        "general",
                        List.of(
                                firstMessage,
                                secondMessage
                        )
                );


        service.ingest(
                batch
        );


        ArgumentCaptor<String> contentCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );


        verify(
                ingestionService
        ).ingest(
                eq(projectId),
                eq(SourceTool.SLACK),
                eq(CollaborationEventType.MESSAGE),
                eq(EventAction.CREATED),
                eq(
                        "slack-batch:channel-a:event-001:event-002"
                ),
                eq(
                        "Slack batch: #general"
                ),
                contentCaptor.capture(),
                isNull(),
                isNull(),
                eq(
                        "#general"
                ),
                isNull(),
                eq(false),
                eq(
                        Instant.parse(
                                "2026-08-16T06:01:00Z"
                        )
                )
        );


        String content =
                contentCaptor.getValue();


        assertTrue(
                content.contains(
                        "alice: API 명세를 변경했습니다."
                )
        );

        assertTrue(
                content.contains(
                        "bob: 백엔드 구현 방향을 수정했습니다."
                )
        );
    }
}