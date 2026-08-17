package com.example.demo.integration.slack;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackBatchSchedulerTest {

    @Test
    void expiredBatchIsPassedToIngestionService() {

        SlackMessageBatchService batchService =
                mock(
                        SlackMessageBatchService.class
                );

        SlackBatchIngestionService batchIngestionService =
                mock(
                        SlackBatchIngestionService.class
                );


        SlackBatchScheduler scheduler =
                new SlackBatchScheduler(
                        batchService,
                        batchIngestionService
                );


        String projectId =
                "11111111-1111-1111-1111-111111111111";


        SlackMessage message =
                new SlackMessage(
                        projectId,
                        "event-expired-001",
                        "channel-a",
                        "general",
                        "6000.001",
                        "user-001",
                        "alice",
                        "API 명세를 변경했습니다.",
                        false,
                        Instant.parse(
                                "2026-08-16T06:00:00Z"
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
                batchService.flushExpired(
                        any(Instant.class)
                )
        ).thenReturn(
                List.of(
                        batch
                )
        );


        scheduler.flushExpiredBatches();


        verify(
                batchService
        ).flushExpired(
                any(Instant.class)
        );


        verify(
                batchIngestionService
        ).ingest(
                batch
        );
    }
}