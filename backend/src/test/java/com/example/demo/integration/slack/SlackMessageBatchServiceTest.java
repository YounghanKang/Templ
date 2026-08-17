package com.example.demo.integration.slack;

import com.example.demo.filter.RuleBasedEventFilter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SlackMessageBatchServiceTest {

    @Test
    void messagesAreBufferedByProjectAndChannel() {

        RuleBasedEventFilter filter =
                new RuleBasedEventFilter();

        SlackMessageBatchService service =
                new SlackMessageBatchService(
                        filter
                );


        String projectId =
                "11111111-1111-1111-1111-111111111111";


        SlackMessage firstMessage =
                new SlackMessage(
                        projectId,
                        "event-001",
                        "channel-a",
                        "general",
                        "1000.001",
                        "user-001",
                        "alice",
                        "API 명세를 수정했습니다.",
                        false,
                        Instant.parse(
                                "2026-08-16T06:00:00Z"
                        )
                );


        SlackMessage secondMessage =
                new SlackMessage(
                        projectId,
                        "event-002",
                        "channel-b",
                        "backend",
                        "1000.002",
                        "user-002",
                        "bob",
                        "배포 일정을 변경했습니다.",
                        false,
                        Instant.parse(
                                "2026-08-16T06:01:00Z"
                        )
                );


        service.accept(
                firstMessage
        );

        service.accept(
                secondMessage
        );


        assertEquals(
                1,
                service.bufferedMessageCount(
                        projectId,
                        "channel-a"
                )
        );

        assertEquals(
                1,
                service.bufferedMessageCount(
                        projectId,
                        "channel-b"
                )
        );
    }
    @Test
    void batchIsCreatedWhenFiveMessagesContainAiRequiredMessage() {

        RuleBasedEventFilter filter =
                new RuleBasedEventFilter();

        SlackMessageBatchService service =
                new SlackMessageBatchService(
                        filter
                );


        String projectId =
                "11111111-1111-1111-1111-111111111111";

        String channelId =
                "channel-a";


        for (int i = 1; i <= 4; i++) {

            SlackMessage message =
                    new SlackMessage(
                            projectId,
                            "event-00" + i,
                            channelId,
                            "general",
                            "1000.00" + i,
                            "user-001",
                            "alice",
                            "프로젝트 진행 상황을 공유드립니다.",
                            false,
                            Instant.parse(
                                    "2026-08-16T06:0" + i + ":00Z"
                            )
                    );


            service.accept(
                    message
            );
        }


        assertEquals(
                4,
                service.bufferedMessageCount(
                        projectId,
                        channelId
                )
        );


        SlackMessage importantMessage =
                new SlackMessage(
                        projectId,
                        "event-005",
                        channelId,
                        "general",
                        "1000.005",
                        "user-002",
                        "bob",
                        "API 명세를 변경했습니다.",
                        false,
                        Instant.parse(
                                "2026-08-16T06:05:00Z"
                        )
                );


        var result =
                service.accept(
                        importantMessage
                );


        assertTrue(
                result.isPresent()
        );

        assertEquals(
                5,
                result.get()
                        .messages()
                        .size()
        );

        assertEquals(
                projectId,
                result.get()
                        .projectId()
        );

        assertEquals(
                channelId,
                result.get()
                        .channelId()
        );

        assertEquals(
                0,
                service.bufferedMessageCount(
                        projectId,
                        channelId
                )
        );
    }
    @Test
    void batchIsNotCreatedWhenFiveMessagesContainNoAiRequiredMessage() {

        RuleBasedEventFilter filter =
                new RuleBasedEventFilter();

        SlackMessageBatchService service =
                new SlackMessageBatchService(
                        filter
                );


        String projectId =
                "11111111-1111-1111-1111-111111111111";

        String channelId =
                "channel-a";


        for (int i = 1; i <= 5; i++) {

            SlackMessage message =
                    new SlackMessage(
                            projectId,
                            "event-general-00" + i,
                            channelId,
                            "general",
                            "2000.00" + i,
                            "user-001",
                            "alice",
                            "프로젝트 진행 상황을 공유드립니다.",
                            false,
                            Instant.parse(
                                    "2026-08-16T07:0" + i + ":00Z"
                            )
                    );


            var result =
                    service.accept(
                            message
                    );


            assertTrue(
                    result.isEmpty()
            );
        }


        assertEquals(
                0,
                service.bufferedMessageCount(
                        projectId,
                        channelId
                )
        );
    }
    @Test
    void expiredBufferCreatesBatchAfterFiveMinutes() {

        RuleBasedEventFilter filter =
                new RuleBasedEventFilter();

        SlackMessageBatchService service =
                new SlackMessageBatchService(
                        filter
                );


        String projectId =
                "11111111-1111-1111-1111-111111111111";

        String channelId =
                "channel-a";

        Instant firstTime =
                Instant.parse(
                        "2026-08-16T06:00:00Z"
                );


        SlackMessage firstMessage =
                new SlackMessage(
                        projectId,
                        "event-expire-001",
                        channelId,
                        "general",
                        "3000.001",
                        "user-001",
                        "alice",
                        "API 명세를 변경했습니다.",
                        false,
                        firstTime
                );


        SlackMessage secondMessage =
                new SlackMessage(
                        projectId,
                        "event-expire-002",
                        channelId,
                        "general",
                        "3000.002",
                        "user-002",
                        "bob",
                        "프로젝트 진행 상황을 공유드립니다.",
                        false,
                        firstTime.plusSeconds(
                                60
                        )
                );


        service.accept(
                firstMessage,
                firstTime
        );

        service.accept(
                secondMessage,
                firstTime.plusSeconds(60)
        );


        assertEquals(
                2,
                service.bufferedMessageCount(
                        projectId,
                        channelId
                )
        );


        var batches =
                service.flushExpired(
                        firstTime.plusSeconds(
                                300
                        )
                );


        assertEquals(
                1,
                batches.size()
        );

        assertEquals(
                2,
                batches.get(0)
                        .messages()
                        .size()
        );

        assertEquals(
                projectId,
                batches.get(0)
                        .projectId()
        );

        assertEquals(
                channelId,
                batches.get(0)
                        .channelId()
        );

        assertEquals(
                0,
                service.bufferedMessageCount(
                        projectId,
                        channelId
                )
        );
    }
    @Test
    void bufferIsNotExpiredBeforeFiveMinutes() {

        RuleBasedEventFilter filter =
                new RuleBasedEventFilter();

        SlackMessageBatchService service =
                new SlackMessageBatchService(
                        filter
                );


        String projectId =
                "11111111-1111-1111-1111-111111111111";

        String channelId =
                "channel-a";

        Instant firstTime =
                Instant.parse(
                        "2026-08-16T06:00:00Z"
                );


        SlackMessage message =
                new SlackMessage(
                        projectId,
                        "event-before-expire-001",
                        channelId,
                        "general",
                        "4000.001",
                        "user-001",
                        "alice",
                        "API 명세를 변경했습니다.",
                        false,
                        firstTime
                );


        service.accept(
                message
        );


        var batches =
                service.flushExpired(
                        firstTime.plusSeconds(
                                299
                        )
                );


        assertTrue(
                batches.isEmpty()
        );

        assertEquals(
                1,
                service.bufferedMessageCount(
                        projectId,
                        channelId
                )
        );
    }
    @Test
    void expiredBufferIsDiscardedWhenNoAiRequiredMessageExists() {

        RuleBasedEventFilter filter =
                new RuleBasedEventFilter();

        SlackMessageBatchService service =
                new SlackMessageBatchService(
                        filter
                );


        String projectId =
                "11111111-1111-1111-1111-111111111111";

        String channelId =
                "channel-a";

        Instant firstTime =
                Instant.parse(
                        "2026-08-16T06:00:00Z"
                );


        SlackMessage firstMessage =
                new SlackMessage(
                        projectId,
                        "event-expire-general-001",
                        channelId,
                        "general",
                        "5000.001",
                        "user-001",
                        "alice",
                        "프로젝트 진행 상황을 공유드립니다.",
                        false,
                        firstTime
                );


        SlackMessage secondMessage =
                new SlackMessage(
                        projectId,
                        "event-expire-general-002",
                        channelId,
                        "general",
                        "5000.002",
                        "user-002",
                        "bob",
                        "오늘 작업 내용을 정리해서 공유드립니다.",
                        false,
                        firstTime.plusSeconds(
                                60
                        )
                );


        service.accept(
                firstMessage,
                firstTime
        );

        service.accept(
                secondMessage,
                firstTime.plusSeconds(60)
        );


        assertEquals(
                2,
                service.bufferedMessageCount(
                        projectId,
                        channelId
                )
        );


        var batches =
                service.flushExpired(
                        firstTime.plusSeconds(
                                300
                        )
                );


        assertTrue(
                batches.isEmpty()
        );

        assertEquals(
                0,
                service.bufferedMessageCount(
                        projectId,
                        channelId
                )
        );
    }
}