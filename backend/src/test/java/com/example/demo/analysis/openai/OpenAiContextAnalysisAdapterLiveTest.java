package com.example.demo.analysis.openai;

import com.example.demo.analysis.AnalysisCandidate;
import com.example.demo.analysis.AnalysisCommand;
import com.example.demo.analysis.ContextAnalysisResult;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiContextAnalysisAdapterLiveTest {

    @Test
    void realOpenAiStructuredOutputCanAnalyzeTaskConflict() {

        UUID projectId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );

        UUID eventId =
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222222"
                );

        UUID loginTaskId =
                UUID.fromString(
                        "33333333-3333-3333-3333-333333333333"
                );

        UUID profileTaskId =
                UUID.fromString(
                        "44444444-4444-4444-4444-444444444444"
                );


        AnalysisCommand command =
                new AnalysisCommand(
                        projectId,
                        eventId,
                        "GITHUB",
                        "backend/auth",
                        "JWT 로그인 기능 삭제",
                        """
                        GitHub 커밋에서 기존 JWT 로그인 API 구현을 삭제했습니다.
                        팀에서 승인된 현재 Task는 'JWT 로그인 API 구현'이며
                        해당 Task 명세는 아직 변경되지 않았습니다.
                        실제 구현 방향과 프로젝트 계획이 서로 충돌하는 상태입니다.
                        """,
                        "backend-developer",
                        List.of(
                                new AnalysisCandidate(
                                        loginTaskId,
                                        "JWT 로그인 API 구현",
                                        95,
                                        List.of(
                                                "JWT",
                                                "로그인",
                                                "API"
                                        )
                                ),
                                new AnalysisCandidate(
                                        profileTaskId,
                                        "사용자 프로필 조회 API 구현",
                                        35,
                                        List.of(
                                                "사용자",
                                                "API"
                                        )
                                )
                        )
                );


        OpenAiContextAnalysisAdapter adapter =
                new OpenAiContextAnalysisAdapter(
                        OpenAIOkHttpClient.fromEnv()
                );


        ReflectionTestUtils.setField(
                adapter,
                "model",
                "gpt-5.6-sol"
        );


        ContextAnalysisResult result =
                adapter.analyze(
                        command
                );


        System.out.println(
                "meaningfulChange = "
                        + result.meaningfulChange()
        );

        System.out.println(
                "changeType = "
                        + result.changeType()
        );

        System.out.println(
                "summary = "
                        + result.summary()
        );

        System.out.println(
                "riskScore = "
                        + result.riskScore()
        );

        System.out.println(
                "confidence = "
                        + result.confidence()
        );

        System.out.println(
                "taskId = "
                        + result.taskId()
        );

        System.out.println(
                "impactedNodeIds = "
                        + result.impactedNodeIds()
        );

        System.out.println(
                "proposedActions = "
                        + result.proposedActions()
        );

        System.out.println(
                "openQuestions = "
                        + result.openQuestions()
        );


        assertTrue(
                result.meaningfulChange()
        );

        assertNotNull(
                result.changeType()
        );

        assertEquals(
                loginTaskId,
                result.taskId()
        );

        assertTrue(
                result.riskScore() >= 0
                        &&
                        result.riskScore() <= 10
        );

        assertTrue(
                result.confidence() >= 0.0
                        &&
                        result.confidence() <= 1.0
        );
    }
}