package com.example.demo.filter;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleBasedEventFilterTest {

    private RuleBasedEventFilter filter;


    @BeforeEach
    void setUp() {
        filter =
                new RuleBasedEventFilter();
    }


    @Test
    void simpleThanksMessageIsFilteredOut() {

        CollaborationEvent event =
                createSlackMessage(
                        "감사합니다",
                        false
                );

        FilterResult result =
                filter.evaluate(event);

        assertEquals(
                FilterDecision.FILTERED_OUT,
                result.decision()
        );
    }


    @Test
    void shortAcknowledgementIsFilteredOut() {

        CollaborationEvent event =
                createSlackMessage(
                        "ㅇㅋ",
                        false
                );

        FilterResult result =
                filter.evaluate(event);

        assertEquals(
                FilterDecision.FILTERED_OUT,
                result.decision()
        );
    }


    @Test
    void apiDeletionRequiresAiAnalysis() {

        CollaborationEvent event =
                createSlackMessage(
                        "API 삭제",
                        false
                );

        FilterResult result =
                filter.evaluate(event);

        assertEquals(
                FilterDecision.AI_REQUIRED,
                result.decision()
        );
    }


    @Test
    void deploymentCancellationRequiresAiAnalysis() {

        CollaborationEvent event =
                createSlackMessage(
                        "배포 취소",
                        false
                );

        FilterResult result =
                filter.evaluate(event);

        assertEquals(
                FilterDecision.AI_REQUIRED,
                result.decision()
        );
    }


    @Test
    void systemGeneratedMessageIsFilteredOut() {

        CollaborationEvent event =
                createSlackMessage(
                        "프로젝트 변경이 승인되었습니다.",
                        true
                );

        FilterResult result =
                filter.evaluate(event);

        assertEquals(
                FilterDecision.FILTERED_OUT,
                result.decision()
        );
    }


    @Test
    void generalConversationIsLogOnly() {

        CollaborationEvent event =
                createSlackMessage(
                        "오늘 회의에서 이야기했던 내용을 다시 확인해 보겠습니다.",
                        false
                );

        FilterResult result =
                filter.evaluate(event);

        assertEquals(
                FilterDecision.LOG_ONLY,
                result.decision()
        );
    }


    private CollaborationEvent createSlackMessage(
            String content,
            boolean generatedBySystem
    ) {

        return new CollaborationEvent(
                UUID.randomUUID(),
                SourceTool.SLACK,
                CollaborationEventType.MESSAGE,
                EventAction.CREATED,
                UUID.randomUUID().toString(),
                null,
                content,
                "U001",
                "테스트 사용자",
                "#backend",
                null,
                generatedBySystem,
                Instant.now()
        );
    }
}