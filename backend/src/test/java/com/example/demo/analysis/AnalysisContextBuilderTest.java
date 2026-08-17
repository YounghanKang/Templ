package com.example.demo.analysis;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import com.example.demo.matching.CandidateNodeMatch;
import com.example.demo.matching.NodeMatchingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisContextBuilderTest {

    private AnalysisContextBuilder builder;


    @BeforeEach
    void setUp() {

        builder =
                new AnalysisContextBuilder();
    }


    @Test
    void eventAndCandidatesAreConvertedToAnalysisCommand() {

        String projectId =
                UUID.randomUUID().toString().toString();

        String nodeId =
                UUID.randomUUID().toString().toString();


        CollaborationEvent event =
                new CollaborationEvent(
                        projectId,
                        SourceTool.SLACK,
                        CollaborationEventType.MESSAGE,
                        EventAction.CREATED,
                        "Ev-analysis-001",
                        null,
                        "로그인 API를 삭제합니다.",
                        "U001",
                        "테스트 사용자",
                        "#backend",
                        null,
                        false,
                        Instant.now()
                );


        CandidateNodeMatch candidate =
                new CandidateNodeMatch(
                        nodeId,
                        "로그인 API 구현",
                        17,
                        List.of(
                                "로그인",
                                "api"
                        )
                );


        NodeMatchingResult matchingResult =
                NodeMatchingResult.mapped(
                        projectId,
                        "#backend",
                        List.of(candidate)
                );


        AnalysisCommand command =
                builder.build(
                        event,
                        matchingResult
                );


        assertEquals(
                projectId,
                command.projectId()
        );


        assertEquals(
                SourceTool.SLACK.name(),
                command.sourceTool()
        );


        assertEquals(
                "로그인 API를 삭제합니다.",
                command.eventContent()
        );


        assertEquals(
                1,
                command.candidates().size()
        );


        assertEquals(
                nodeId,
                command.candidates()
                        .get(0)
                        .nodeId()
        );


        assertEquals(
                17,
                command.candidates()
                        .get(0)
                        .matchScore()
        );
    }


    @Test
    void nullEventIsRejected() {

        String projectId =
                UUID.randomUUID().toString().toString();


        NodeMatchingResult matchingResult =
                NodeMatchingResult.unmapped(
                        projectId,
                        "#backend"
                );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                builder.build(
                                        null,
                                        matchingResult
                                )
                );


        assertEquals(
                "event는 필수입니다.",
                exception.getMessage()
        );
    }
}