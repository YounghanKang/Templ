package com.example.demo.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NodeMatchingServiceTest {

    private String projectId;

    private NodeContext loginNode;

    private NodeContext paymentNode;

    private FakeProjectContextReader
            projectContextReader;

    private NodeMatchingService
            nodeMatchingService;


    @BeforeEach
    void setUp() {

        projectId =
                UUID.randomUUID().toString();


        loginNode =
                new NodeContext(
                        UUID.randomUUID().toString(),
                        "로그인 API 구현",
                        "사용자 로그인 기능을 완성한다.",
                        "JWT 기반 인증 API를 구현한다.",
                        List.of(
                                "로그인",
                                "인증",
                                "API",
                                "JWT"
                        )
                );


        paymentNode =
                new NodeContext(
                        UUID.randomUUID().toString(),
                        "결제 화면 구현",
                        "결제 UI를 완성한다.",
                        "결제 페이지를 구현한다.",
                        List.of(
                                "결제",
                                "UI"
                        )
                );


        projectContextReader =
                new FakeProjectContextReader(
                        List.of(
                                paymentNode,
                                loginNode
                        )
                );


        nodeMatchingService =
                new NodeMatchingService(
                        projectContextReader,
                        new CandidateNodeMatcher()
                );
    }


    @Test
    void relatedNodeIsReturnedAsMapped() {

        NodeMatchingResult result =
                nodeMatchingService.match(
                        projectId,
                        "C123BACKEND",
                        "로그인 API를 삭제하기로 했습니다."
                );


        assertTrue(
                result.mapped()
        );


        assertFalse(
                result.candidates()
                        .isEmpty()
        );


        assertEquals(
                loginNode.nodeId(),
                result.candidates()
                        .get(0)
                        .nodeId()
        );


        assertEquals(
                projectId,
                result.projectId()
        );


        assertEquals(
                "C123BACKEND",
                result.channelId()
        );
    }


    @Test
    void unrelatedMessageBecomesUnmapped() {

        NodeMatchingResult result =
                nodeMatchingService.match(
                        projectId,
                        "C123BACKEND",
                        "오늘 점심 메뉴를 정해 주세요."
                );


        assertFalse(
                result.mapped()
        );


        assertTrue(
                result.candidates()
                        .isEmpty()
        );
    }


    @Test
    void emptyCandidateListBecomesUnmapped() {

        FakeProjectContextReader emptyReader =
                new FakeProjectContextReader(
                        List.of()
                );


        NodeMatchingService service =
                new NodeMatchingService(
                        emptyReader,
                        new CandidateNodeMatcher()
                );


        NodeMatchingResult result =
                service.match(
                        projectId,
                        "C123BACKEND",
                        "로그인 API를 삭제합니다."
                );


        assertFalse(
                result.mapped()
        );


        assertTrue(
                result.candidates()
                        .isEmpty()
        );
    }


    @Test
    void projectIdIsRequired() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                nodeMatchingService.match(
                                        null,
                                        "C123BACKEND",
                                        "로그인 API 변경"
                                )
                );


        assertEquals(
                "projectId는 필수입니다.",
                exception.getMessage()
        );
    }


    /*
     * Backend1의 실제 구현체 대신 사용하는
     * 테스트 전용 Fake Adapter입니다.
     */
    private static class FakeProjectContextReader
            implements ProjectContextReader {

        private final List<NodeContext>
                candidateNodes;


        private FakeProjectContextReader(
                List<NodeContext> candidateNodes
        ) {

            this.candidateNodes =
                    candidateNodes;
        }


        @Override
        public ProjectContext getProjectContext(
                String projectId
        ) {

            return new ProjectContext(
                    projectId,
                    "테스트 프로젝트",
                    "Backend2 테스트",
                    1L,
                    candidateNodes
            );
        }


        @Override
        public List<NodeContext> findCandidateNodes(
                String projectId,
                String channelId
        ) {

            return candidateNodes;
        }


        @Override
        public List<NodeRelation> getRelatedNodes(
                String projectId,
                String nodeId
        ) {

            return List.of();
        }
    }
}
