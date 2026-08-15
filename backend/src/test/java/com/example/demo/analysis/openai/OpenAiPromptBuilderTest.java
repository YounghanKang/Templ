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
    void promptContainsEventAndCandidates() {

        UUID projectId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        UUID nodeId =
                UUID.randomUUID();


        AnalysisCandidate candidate =
                new AnalysisCandidate(
                        nodeId,
                        "로그인 API 구현",
                        85,
                        List.of(
                                "로그인",
                                "API"
                        )
                );


        AnalysisCommand command =
                new AnalysisCommand(
                        projectId,
                        eventId,
                        "SLACK",
                        "#backend",
                        null,
                        "로그인 API를 삭제하기로 결정했습니다.",
                        "테스트 사용자",
                        List.of(candidate)
                );


        String prompt =
                promptBuilder.build(command);


        assertTrue(
                prompt.contains(
                        projectId.toString()
                )
        );

        assertTrue(
                prompt.contains(
                        eventId.toString()
                )
        );

        assertTrue(
                prompt.contains(
                        nodeId.toString()
                )
        );

        assertTrue(
                prompt.contains(
                        "로그인 API 구현"
                )
        );

        assertTrue(
                prompt.contains(
                        "로그인 API를 삭제하기로 결정했습니다."
                )
        );

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