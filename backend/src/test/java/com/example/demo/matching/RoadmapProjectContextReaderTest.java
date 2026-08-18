package com.example.demo.matching;

import com.example.demo.domain.RoadmapEdge;
import com.example.demo.domain.RoadmapNode;
import com.example.demo.domain.Team;
import com.example.demo.repository.RoadmapEdgeRepository;
import com.example.demo.repository.RoadmapNodeRepository;
import com.example.demo.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RoadmapProjectContextReaderTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RoadmapNodeRepository roadmapNodeRepository;

    @Autowired
    private RoadmapEdgeRepository roadmapEdgeRepository;

    private RoadmapProjectContextReader reader;


    @BeforeEach
    void setUp() {

        roadmapEdgeRepository.deleteAll();
        roadmapNodeRepository.deleteAll();
        teamRepository.deleteAll();

        reader =
                new RoadmapProjectContextReader(
                        teamRepository,
                        roadmapNodeRepository,
                        roadmapEdgeRepository
                );

        Team team =
                Team.builder()
                        .teamId("T-TEST")
                        .name("TEMPL Test Team")
                        .members(4)
                        .color("#000000")
                        .mission("협업 진행 상황과 계획의 차이를 빠르게 감지한다.")
                        .build();

        teamRepository.save(team);


        RoadmapNode root =
                RoadmapNode.builder()
                        .nodeId("root")
                        .teamId("T-TEST")
                        .label("프로젝트 완성")
                        .goal("전체 프로젝트를 완성한다.")
                        .status("active")
                        .progress(40)
                        .aiSummary("전체 프로젝트 루트 작업")
                        .tier("root")
                        .assigneesJson("[]")
                        .prerequisitesJson("[]")
                        .build();

        RoadmapNode backend =
                RoadmapNode.builder()
                        .nodeId("m2")
                        .teamId("T-TEST")
                        .label("백엔드 API 구현")
                        .code("BE-02")
                        .goal("Spring Boot 기반 API를 구현한다.")
                        .status("active")
                        .progress(55)
                        .dueDate("2026-08-20")
                        .aiSummary("GitHub와 Slack 진행 상황을 분석하는 백엔드 작업")
                        .issue("Webhook 처리 지연 점검 필요")
                        .tier("mid")
                        .assigneesJson("[\"Alice\", \"Bob\"]")
                        .prerequisitesJson("[\"프로젝트 설계\"]")
                        .build();

        roadmapNodeRepository.saveAll(
                List.of(
                        root,
                        backend
                )
        );


        RoadmapEdge edge =
                RoadmapEdge.builder()
                        .edgeId("e-root-m2")
                        .teamId("T-TEST")
                        .fromNodeId("root")
                        .toNodeId("m2")
                        .build();

        roadmapEdgeRepository.save(edge);
    }


    @Test
    void be1RoadmapCanBeReadAsBe2ProjectContext() {

        ProjectContext context =
                reader.getProjectContext(
                        "T-TEST"
                );

        assertEquals(
                "T-TEST",
                context.projectId()
        );

        assertEquals(
                "TEMPL Test Team",
                context.projectName()
        );

        assertEquals(
                "협업 진행 상황과 계획의 차이를 빠르게 감지한다.",
                context.projectSummary()
        );

        assertEquals(
                2,
                context.nodes().size()
        );


        NodeContext backendNode =
                context
                        .nodes()
                        .stream()
                        .filter(
                                node ->
                                        "m2".equals(
                                                node.nodeId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();


        assertEquals(
                "백엔드 API 구현",
                backendNode.title()
        );

        assertEquals(
                "Spring Boot 기반 API를 구현한다.",
                backendNode.goal()
        );

        assertEquals(
                "GitHub와 Slack 진행 상황을 분석하는 백엔드 작업",
                backendNode.summary()
        );

        assertTrue(
                backendNode
                        .keywords()
                        .contains("BE-02")
        );

        assertTrue(
                backendNode
                        .keywords()
                        .contains("Webhook 처리 지연 점검 필요")
        );

        assertTrue(
                backendNode
                        .keywords()
                        .contains("Alice")
        );

        assertTrue(
                backendNode
                        .keywords()
                        .contains("Bob")
        );

        assertTrue(
                backendNode
                        .keywords()
                        .contains("프로젝트 설계")
        );


        List<NodeRelation> relations =
                reader.getRelatedNodes(
                        "T-TEST",
                        "m2"
                );

        assertEquals(
                1,
                relations.size()
        );

        assertEquals(
                "root",
                relations.get(0).fromNodeId()
        );

        assertEquals(
                "m2",
                relations.get(0).toNodeId()
        );

        assertEquals(
                "DEPENDENCY",
                relations.get(0).relationType()
        );
    }
    @Test
    void collaborationMessageCanBeMatchedToActualBe1RoadmapNode() {

        NodeMatchingService matchingService =
                new NodeMatchingService(
                        reader,
                        new CandidateNodeMatcher()
                );


        NodeMatchingResult result =
                matchingService.match(
                        "T-TEST",
                        "C-TEST",
                        "Alice Webhook 처리 지연 문제를 수정 중"
                );


        assertTrue(
                result.mapped()
        );

        assertFalse(
                result.candidates().isEmpty()
        );


        CandidateNodeMatch firstCandidate =
                result.candidates().get(0);


        assertEquals(
                "m2",
                firstCandidate.nodeId()
        );

        assertEquals(
                "백엔드 API 구현",
                firstCandidate.title()
        );

        assertTrue(
                firstCandidate.score() > 0
        );

        assertTrue(
                firstCandidate
                        .matchedTerms()
                        .contains("webhook")
        );
    }
}