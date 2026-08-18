package com.example.demo.integration.slack;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackMessageProcessingServiceTest {

    @Test
    void completedBatchIsPassedToIngestionService() {

        SlackMessageBatchService batchService =
                mock(
                        SlackMessageBatchService.class
                );

        SlackBatchIngestionService batchIngestionService =
                mock(
                        SlackBatchIngestionService.class
                );


        SlackMessageProcessingService service =
                new SlackMessageProcessingService(
                        batchService,
                        batchIngestionService
                );


        String projectId =
                "11111111-1111-1111-1111-111111111111";


        SlackMessage message =
                new SlackMessage(
                        projectId,
                        "event-005",
                        "channel-a",
                        "general",
                        "1000.005",
                        "user-001",
                        "alice",
                        "API 명세를 변경했습니다.",
                        false,
                        Instant.parse(
                                "2026-08-16T06:05:00Z"
                        )
                );


        SlackMessageBatch batch =
                new SlackMessageBatch(
                        projectId,
                        "channel-a",
                        "general",
                        List.of(
                                message
                        )
                );


        when(
                batchService.accept(
                        message
                )
        ).thenReturn(
                Optional.of(
                        batch
                )
        );


        service.process(
                message
        );


        verify(
                batchService
        ).accept(
                message
        );


        verify(
                batchIngestionService
        ).ingest(
                batch
        );
    }
}