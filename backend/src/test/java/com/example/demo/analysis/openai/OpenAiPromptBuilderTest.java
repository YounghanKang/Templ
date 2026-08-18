package com.example.demo.analysis.openai;

import com.example.demo.analysis.AnalysisCandidate;
import com.example.demo.analysis.AnalysisCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiPromptBuilderTest {

    private final OpenAiPromptBuilder
            promptBuilder =
            new OpenAiPromptBuilder();


    @Test
    void promptContainsEventAndActualProjectTaskContext() {

        String projectId =
                "T-001";

        UUID eventId =
                UUID.randomUUID();

        String nodeId =
                "m2";


        AnalysisCandidate candidate =
                new AnalysisCandidate(
                        nodeId,
                        "백엔드 API 구현",
                        "Spring Boot 기반 API를 구현한다.",
                        "GitHub와 Slack 진행 상황을 분석하는 백엔드 작업",
                        "IN_PROGRESS",
                        55,
                        List.of(
                                "Alice",
                                "Bob"
                        ),
                        List.of(
                                "프로젝트 설계"
                        ),
                        "2026-08-20",
                        85,
                        List.of(
                                "webhook",
                                "api"
                        )
                );


        AnalysisCommand command =
                new AnalysisCommand(
                        projectId,
                        "TEMPL",
                        "계획과 실제 협업 진행 상황의 차이를 감지한다.",
                        eventId,
                        "SLACK",
                        "#backend",
                        null,
                        "Alice가 Webhook 처리 지연 문제를 수정하고 있습니다.",
                        "Alice",
                        List.of(candidate)
                );


        String prompt =
                promptBuilder.build(command);


        /*
         * Project Context
         */
        assertTrue(
                prompt.contains(
                        "TEMPL"
                )
        );

        assertTrue(
                prompt.contains(
                        "계획과 실제 협업 진행 상황의 차이를 감지한다."
                )
        );


        /*
         * Event Context
         */
        assertTrue(
                prompt.contains(
                        projectId
                )
        );

        assertTrue(
                prompt.contains(
                        eventId.toString()
                )
        );

        assertTrue(
                prompt.contains(
                        "SLACK"
                )
        );

        assertTrue(
                prompt.contains(
                        "Alice가 Webhook 처리 지연 문제를 수정하고 있습니다."
                )
        );


        /*
         * Task Context
         */
        assertTrue(
                prompt.contains(
                        nodeId
                )
        );

        assertTrue(
                prompt.contains(
                        "백엔드 API 구현"
                )
        );

        assertTrue(
                prompt.contains(
                        "Spring Boot 기반 API를 구현한다."
                )
        );

        assertTrue(
                prompt.contains(
                        "GitHub와 Slack 진행 상황을 분석하는 백엔드 작업"
                )
        );

        assertTrue(
                prompt.contains(
                        "IN_PROGRESS"
                )
        );

        assertTrue(
                prompt.contains(
                        "55"
                )
        );

        assertTrue(
                prompt.contains(
                        "Alice"
                )
        );

        assertTrue(
                prompt.contains(
                        "Bob"
                )
        );

        assertTrue(
                prompt.contains(
                        "프로젝트 설계"
                )
        );

        assertTrue(
                prompt.contains(
                        "2026-08-20"
                )
        );


        /*
         * 분석 규칙
         */
        assertTrue(
                prompt.contains(
                        "SCOPE_DRIFT"
                )
        );

        assertTrue(
                prompt.contains(
                        "DEPENDENCY_BREAK"
                )
        );

        assertTrue(
                prompt.contains(
                        "계획된 목표(goal)와 실제 수행 내용의 차이"
                )
        );

        assertTrue(
                prompt.contains(
                        "존재하지 않는 nodeId를 새로 만들어내지 마세요."
                )
        );
    }


    @Test
    void nullCommandIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        promptBuilder.build(
                                null
                        )
        );
    }
}