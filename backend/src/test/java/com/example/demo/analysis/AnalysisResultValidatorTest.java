package com.example.demo.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisResultValidatorTest {

    private AnalysisResultValidator validator;

    private UUID projectId;

    private UUID eventId;

    private UUID candidateNodeId;

    private AnalysisCommand command;


    @BeforeEach
    void setUp() {

        validator =
                new AnalysisResultValidator();


        projectId =
                UUID.randomUUID();

        eventId =
                UUID.randomUUID();

        candidateNodeId =
                UUID.randomUUID();


        AnalysisCandidate candidate =
                new AnalysisCandidate(
                        candidateNodeId,
                        "로그인 API 구현",
                        17,
                        List.of(
                                "로그인",
                                "api"
                        )
                );


        command =
                new AnalysisCommand(
                        projectId,
                        eventId,
                        "SLACK",
                        "#backend",
                        null,
                        "로그인 API를 삭제합니다.",
                        "테스트 사용자",
                        List.of(candidate)
                );
    }


    @Test
    void validAnalysisResultPassesValidation() {

        ContextAnalysisResult result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.SCOPE_DRIFT,
                        "로그인 API 삭제가 기존 작업 범위를 변경합니다.",
                        8,
                        0.91,
                        candidateNodeId,
                        List.of(),
                        List.of(
                                "로그인 API 작업 목표를 검토한다."
                        ),
                        List.of(
                                "인증 기능도 함께 제거합니까?"
                        )
                );


        assertDoesNotThrow(
                () ->
                        validator.validate(
                                command,
                                result
                        )
        );
    }


    @Test
    void riskScoreGreaterThanTenIsRejected() {

        ContextAnalysisResult result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.SCOPE_DRIFT,
                        "테스트",
                        11,
                        0.9,
                        candidateNodeId,
                        List.of(),
                        List.of(),
                        List.of()
                );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        command,
                                        result
                                )
                );


        assertEquals(
                "riskScore는 1 이상 10 이하여야 합니다.",
                exception.getMessage()
        );
    }


    @Test
    void confidenceOutsideRangeIsRejected() {

        ContextAnalysisResult result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.SCOPE_DRIFT,
                        "테스트",
                        8,
                        1.2,
                        candidateNodeId,
                        List.of(),
                        List.of(),
                        List.of()
                );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        command,
                                        result
                                )
                );


        assertEquals(
                "confidence는 0 이상 1 이하여야 합니다.",
                exception.getMessage()
        );
    }


    @Test
    void unknownPrimaryNodeIsRejected() {

        ContextAnalysisResult result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.SCOPE_DRIFT,
                        "테스트",
                        8,
                        0.9,
                        UUID.randomUUID(),
                        List.of(),
                        List.of(),
                        List.of()
                );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        command,
                                        result
                                )
                );


        assertEquals(
                "primaryNodeId가 후보 Node에 존재하지 않습니다.",
                exception.getMessage()
        );
    }
}