package com.example.demo.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CandidateNodeMatcherTest {

    private CandidateNodeMatcher matcher;


    @BeforeEach
    void setUp() {

        matcher =
                new CandidateNodeMatcher();
    }


    @Test
    void loginApiMessageRanksLoginTaskFirst() {

        NodeContext loginNode =
                new NodeContext(
                        UUID.randomUUID(),
                        "로그인 API 구현",
                        "사용자 인증 API를 완성한다.",
                        "로그인과 JWT 인증을 구현한다.",
                        List.of(
                                "로그인",
                                "인증",
                                "API"
                        )
                );


        NodeContext paymentNode =
                new NodeContext(
                        UUID.randomUUID(),
                        "결제 화면 디자인",
                        "결제 UI를 완성한다.",
                        "결제 화면과 버튼을 구현한다.",
                        List.of(
                                "결제",
                                "UI"
                        )
                );


        NodeContext databaseNode =
                new NodeContext(
                        UUID.randomUUID(),
                        "DB 스키마 설계",
                        "사용자 테이블 구조를 정의한다.",
                        "데이터베이스 구조를 설계한다.",
                        List.of(
                                "DB",
                                "schema",
                                "스키마"
                        )
                );


        List<CandidateNodeMatch> result =
                matcher.findTopCandidates(
                        "로그인 API 삭제 예정입니다.",
                        List.of(
                                paymentNode,
                                databaseNode,
                                loginNode
                        )
                );


        assertFalse(
                result.isEmpty()
        );


        assertEquals(
                loginNode.nodeId(),
                result.get(0).nodeId()
        );


        assertTrue(
                result.get(0).score() > 0
        );


        assertTrue(
                result.get(0)
                        .matchedTerms()
                        .contains("로그인")
        );
    }


    @Test
    void unrelatedNodesAreExcluded() {

        NodeContext paymentNode =
                new NodeContext(
                        UUID.randomUUID(),
                        "결제 화면 디자인",
                        "결제 UI를 완성한다.",
                        "결제 화면을 구현한다.",
                        List.of(
                                "결제",
                                "UI"
                        )
                );


        List<CandidateNodeMatch> result =
                matcher.findTopCandidates(
                        "로그인 API를 삭제합니다.",
                        List.of(
                                paymentNode
                        )
                );


        assertTrue(
                result.isEmpty()
        );
    }


    @Test
    void resultIsLimitedToFiveCandidates() {

        List<NodeContext> nodes =
                List.of(
                        createApiNode("API 작업 1"),
                        createApiNode("API 작업 2"),
                        createApiNode("API 작업 3"),
                        createApiNode("API 작업 4"),
                        createApiNode("API 작업 5"),
                        createApiNode("API 작업 6"),
                        createApiNode("API 작업 7")
                );


        List<CandidateNodeMatch> result =
                matcher.findTopCandidates(
                        "API 변경",
                        nodes
                );


        assertEquals(
                5,
                result.size()
        );
    }


    private NodeContext createApiNode(
            String title
    ) {

        return new NodeContext(
                UUID.randomUUID(),
                title,
                "API 기능을 구현한다.",
                "API 관련 작업",
                List.of(
                        "API"
                )
        );
    }
}