package com.example.demo.analysis;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import com.example.demo.matching.CandidateNodeMatcher;
import com.example.demo.matching.NodeContext;
import com.example.demo.matching.NodeMatchingService;
import com.example.demo.matching.NodeRelation;
import com.example.demo.matching.ProjectContext;
import com.example.demo.matching.ProjectContextReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyzeEventCoreTest {

    private String projectId;

    private String loginNodeId;

    private FakeProjectContextReader
            projectContextReader;

    private FakeContextAnalysisPort
            analysisPort;

    private AnalyzeEventCore
            analyzeEventCore;


    @BeforeEach
    void setUp() {

        projectId =
                UUID.randomUUID().toString();


        loginNodeId =
                UUID.randomUUID().toString();


        NodeContext loginNode =
                new NodeContext(
                        loginNodeId,
                        "로그인 API 구현",
                        "사용자 로그인 API를 완성한다.",
                        "JWT 인증 기반 로그인 기능을 구현한다.",
                        List.of(
                                "로그인",
                                "인증",
                                "API",
                                "JWT"
                        )
                );


        projectContextReader =
                new FakeProjectContextReader(
                        List.of(loginNode)
                );


        NodeMatchingService nodeMatchingService =
                new NodeMatchingService(
                        projectContextReader,
                        new CandidateNodeMatcher()
                );


        analysisPort =
                new FakeContextAnalysisPort();


        analyzeEventCore =
                new AnalyzeEventCore(
                        nodeMatchingService,
                        new AnalysisContextBuilder(
                                projectContextReader
                        ),
                        analysisPort,
                        new AnalysisResultValidator()
                );
    }


    @Test
    void highRiskChangeCreatesHardWarningOutcome() {

        analysisPort.result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.SCOPE_DRIFT,
                        "로그인 API 삭제가 기존 작업 범위를 변경합니다.",
                        8,
                        0.95,
                        loginNodeId,
                        List.of(),
                        List.of(
                                "로그인 API 작업 목표를 검토한다."
                        ),
                        List.of()
                );


        AnalyzeExecutionOutcome outcome =
                analyzeEventCore.execute(
                        createEvent(
                                "로그인 API를 삭제합니다."
                        )
                );


        assertEquals(
                AnalysisOutcomeType.HARD_WARNING,
                outcome.outcomeType()
        );


        assertEquals(
                1,
                analysisPort.callCount
        );


        assertEquals(
                loginNodeId,
                outcome.analysisResult()
                        .taskId()
        );
    }


    @Test
    void mediumRiskChangeCreatesSoftWarningOutcome() {

        analysisPort.result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.DEPENDENCY_BREAK,
                        "로그인 API 변경의 영향 확인이 필요합니다.",
                        7,
                        0.88,
                        loginNodeId,
                        List.of(),
                        List.of(),
                        List.of()
                );


        AnalyzeExecutionOutcome outcome =
                analyzeEventCore.execute(
                        createEvent(
                                "로그인 API를 변경합니다."
                        )
                );


        assertEquals(
                AnalysisOutcomeType.SOFT_WARNING,
                outcome.outcomeType()
        );
    }


    @Test
    void nonMeaningfulChangeCreatesNoWarningOutcome() {

        analysisPort.result =
                new ContextAnalysisResult(
                        false,
                        null,
                        "프로젝트 구조에 의미 있는 변경이 아닙니다.",
                        2,
                        0.93,
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );


        AnalyzeExecutionOutcome outcome =
                analyzeEventCore.execute(
                        createEvent(
                                "로그인 API 작업 확인했습니다."
                        )
                );


        assertEquals(
                AnalysisOutcomeType.NO_WARNING,
                outcome.outcomeType()
        );
    }


    @Test
    void unrelatedEventBecomesUnmappedWithoutCallingAi() {

        AnalyzeExecutionOutcome outcome =
                analyzeEventCore.execute(
                        createEvent(
                                "오늘 점심 메뉴를 정합시다."
                        )
                );


        assertEquals(
                AnalysisOutcomeType.UNMAPPED,
                outcome.outcomeType()
        );


        assertEquals(
                0,
                analysisPort.callCount
        );


        assertNull(
                outcome.command()
        );


        assertNull(
                outcome.analysisResult()
        );
    }


    @Test
    void invalidAiNodeIdIsRejected() {

        analysisPort.result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.SCOPE_DRIFT,
                        "테스트",
                        8,
                        0.9,
                        UUID.randomUUID().toString(),
                        List.of(),
                        List.of(),
                        List.of()
                );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        analyzeEventCore.execute(
                                createEvent(
                                        "로그인 API를 삭제합니다."
                                )
                        )
        );
    }


    private CollaborationEvent createEvent(
            String content
    ) {

        return new CollaborationEvent(
                projectId,
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
                false,
                Instant.now()
        );
    }


    private static class FakeProjectContextReader
            implements ProjectContextReader {

        private final List<NodeContext>
                candidates;


        private FakeProjectContextReader(
                List<NodeContext> candidates
        ) {

            this.candidates =
                    candidates;
        }


        @Override
        public ProjectContext getProjectContext(
                String projectId
        ) {

            return new ProjectContext(
                    projectId,
                    "테스트 프로젝트",
                    "Analyze Core 테스트",
                    1L,
                    candidates
            );
        }


        @Override
        public List<NodeContext> findCandidateNodes(
                String projectId,
                String channelId
        ) {

            return candidates;
        }


        @Override
        public List<NodeRelation> getRelatedNodes(
                String projectId,
                String nodeId
        ) {

            return List.of();
        }
    }


    private static class FakeContextAnalysisPort
            implements ContextAnalysisPort {

        private ContextAnalysisResult result;

        private int callCount = 0;


        @Override
        public ContextAnalysisResult analyze(
                AnalysisCommand command
        ) {

            callCount++;

            if (result == null) {

                throw new IllegalStateException(
                        "Fake AI 결과가 설정되지 않았습니다."
                );
            }


            return result;
        }
    }
}
