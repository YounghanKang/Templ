package com.example.demo.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EventAnalysisRepositoryTest {

    @Autowired
    private EventAnalysisRepository repository;


    @BeforeEach
    void cleanUp() {

        repository.deleteAll();
    }


    @Test
    void analysisResultCanBeStoredAndRead() {

        UUID eventId =
                UUID.randomUUID();

        UUID nodeId =
                UUID.randomUUID();


        EventAnalysis analysis =
                new EventAnalysis(
                        eventId,
                        1,
                        AnalysisOutcomeType.HARD_WARNING,
                        true,
                        ChangeType.SCOPE_DRIFT,
                        "로그인 API 삭제가 기존 작업 범위를 변경합니다.",
                        8,
                        0.95,
                        nodeId,
                        "fake-model",
                        "v1",
                        "v1"
                );


        EventAnalysis saved =
                repository.save(analysis);


        assertNotNull(
                saved.getId()
        );


        EventAnalysis found =
                repository
                        .findTopByCollaborationEventIdOrderByCreatedAtDesc(
                                eventId
                        )
                        .orElseThrow();


        assertEquals(
                eventId,
                found.getCollaborationEventId()
        );


        assertEquals(
                1,
                found.getRunNumber()
        );


        assertEquals(
                AnalysisOutcomeType.HARD_WARNING,
                found.getOutcomeType()
        );


        assertEquals(
                ChangeType.SCOPE_DRIFT,
                found.getChangeType()
        );


        assertEquals(
                8,
                found.getRiskScore()
        );


        assertEquals(
                0.95,
                found.getConfidence(),
                0.0001
        );


        assertEquals(
                nodeId,
                found.getPrimaryNodeId()
        );
    }


    @Test
    void eventAndRunNumberCombinationCanBeChecked() {

        UUID eventId =
                UUID.randomUUID();


        repository.save(
                new EventAnalysis(
                        eventId,
                        1,
                        AnalysisOutcomeType.SOFT_WARNING,
                        true,
                        ChangeType.DEPENDENCY_BREAK,
                        "의존성 변경 확인이 필요합니다.",
                        7,
                        0.88,
                        UUID.randomUUID(),
                        "fake-model",
                        "v1",
                        "v1"
                )
        );


        assertTrue(
                repository
                        .existsByCollaborationEventIdAndRunNumber(
                                eventId,
                                1
                        )
        );


        assertFalse(
                repository
                        .existsByCollaborationEventIdAndRunNumber(
                                eventId,
                                2
                        )
        );
    }
}