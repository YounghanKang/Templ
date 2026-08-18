package com.example.demo.job;

import com.example.demo.analysis.AnalysisOutcomeType;
import com.example.demo.analysis.AnalyzeExecutionOutcome;
import com.example.demo.analysis.ChangeType;
import com.example.demo.analysis.ContextAnalysisResult;
import com.example.demo.analysis.EventAnalysis;
import com.example.demo.analysis.EventAnalysisRepository;
import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import com.example.demo.matching.NodeMatchingResult;
import com.example.demo.signal.SignalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        AnalyzeJobClaimService.class,
        AnalyzeJobCompletionService.class
})
class AnalyzeJobCompletionServiceTest {

    @Autowired
    private AnalyzeJobClaimService claimService;

    @Autowired
    private AnalyzeJobCompletionService completionService;

    @Autowired
    private CollaborationEventRepository eventRepository;

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private EventAnalysisRepository analysisRepository;


    @BeforeEach
    void cleanUp() {

        analysisRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }


    @Test
    void hardWarningCompletesJobAndCreatesWarning() {

        CollaborationEvent savedEvent =
                eventRepository.save(
                        createEvent(
                                "로그인 API를 삭제합니다."
                        )
                );


        UUID jobId =
                createAndClaimAnalyzeJob(
                        savedEvent.getId()
                );


        String nodeId =
                UUID.randomUUID().toString().toString();


        ContextAnalysisResult result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.SCOPE_DRIFT,
                        "로그인 API 삭제가 기존 작업 범위를 변경합니다.",
                        8,
                        0.95,
                        nodeId,
                        List.of(),
                        List.of(),
                        List.of()
                );


        AnalyzeExecutionOutcome outcome =
                AnalyzeExecutionOutcome.analyzed(
                        AnalysisOutcomeType.HARD_WARNING,
                        NodeMatchingResult.mapped(
                                savedEvent.getProjectId(),
                                "#backend",
                                List.of()
                        ),
                        null,
                        result
                );


        completionService.complete(
                jobId,
                outcome
        );


        ProcessingJob completedJob =
                jobRepository
                        .findById(jobId)
                        .orElseThrow();


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(
                                savedEvent.getId()
                        )
                        .orElseThrow();


        EventAnalysis savedAnalysis =
                analysisRepository
                        .findTopByCollaborationEventIdOrderByCreatedAtDesc(
                                savedEvent.getId()
                        )
                        .orElseThrow();


        assertEquals(
                JobStatus.COMPLETED,
                completedJob.getStatus()
        );


        assertNotNull(
                completedJob.getCompletedAt()
        );


        assertEquals(
                SignalStatus.WARNING_CREATED,
                updatedEvent.getStatus()
        );


        assertEquals(
                AnalysisOutcomeType.HARD_WARNING,
                savedAnalysis.getOutcomeType()
        );


        assertEquals(
                8,
                savedAnalysis.getRiskScore()
        );


        assertEquals(
                nodeId,
                savedAnalysis.getTaskId()
        );
    }


    @Test
    void softWarningCreatesWarningStatus() {

        CollaborationEvent savedEvent =
                eventRepository.save(
                        createEvent(
                                "로그인 API 변경이 필요합니다."
                        )
                );


        UUID jobId =
                createAndClaimAnalyzeJob(
                        savedEvent.getId()
                );


        ContextAnalysisResult result =
                new ContextAnalysisResult(
                        true,
                        ChangeType.DEPENDENCY_BREAK,
                        "관련 작업 영향 확인이 필요합니다.",
                        7,
                        0.88,
                        UUID.randomUUID().toString(),
                        List.of(),
                        List.of(),
                        List.of()
                );


        AnalyzeExecutionOutcome outcome =
                AnalyzeExecutionOutcome.analyzed(
                        AnalysisOutcomeType.SOFT_WARNING,
                        NodeMatchingResult.mapped(
                                savedEvent.getProjectId(),
                                "#backend",
                                List.of()
                        ),
                        null,
                        result
                );


        completionService.complete(
                jobId,
                outcome
        );


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(
                                savedEvent.getId()
                        )
                        .orElseThrow();


        assertEquals(
                SignalStatus.WARNING_CREATED,
                updatedEvent.getStatus()
        );


        assertEquals(
                JobStatus.COMPLETED,
                jobRepository
                        .findById(jobId)
                        .orElseThrow()
                        .getStatus()
        );
    }


    @Test
    void noWarningBecomesAnalyzed() {

        CollaborationEvent savedEvent =
                eventRepository.save(
                        createEvent(
                                "로그인 API 작업을 확인했습니다."
                        )
                );


        UUID jobId =
                createAndClaimAnalyzeJob(
                        savedEvent.getId()
                );


        ContextAnalysisResult result =
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
                AnalyzeExecutionOutcome.analyzed(
                        AnalysisOutcomeType.NO_WARNING,
                        NodeMatchingResult.mapped(
                                savedEvent.getProjectId(),
                                "#backend",
                                List.of()
                        ),
                        null,
                        result
                );


        completionService.complete(
                jobId,
                outcome
        );


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(
                                savedEvent.getId()
                        )
                        .orElseThrow();


        assertEquals(
                SignalStatus.ANALYZED,
                updatedEvent.getStatus()
        );


        assertEquals(
                JobStatus.COMPLETED,
                jobRepository
                        .findById(jobId)
                        .orElseThrow()
                        .getStatus()
        );
    }


    @Test
    void unmappedEventBecomesUnmappedAndJobCompletes() {

        CollaborationEvent savedEvent =
                eventRepository.save(
                        createEvent(
                                "오늘 점심 메뉴를 정합니다."
                        )
                );


        UUID jobId =
                createAndClaimAnalyzeJob(
                        savedEvent.getId()
                );


        AnalyzeExecutionOutcome outcome =
                AnalyzeExecutionOutcome.unmapped(
                        NodeMatchingResult.unmapped(
                                savedEvent.getProjectId(),
                                "#backend"
                        )
                );


        completionService.complete(
                jobId,
                outcome
        );


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(
                                savedEvent.getId()
                        )
                        .orElseThrow();


        ProcessingJob completedJob =
                jobRepository
                        .findById(jobId)
                        .orElseThrow();


        EventAnalysis savedAnalysis =
                analysisRepository
                        .findTopByCollaborationEventIdOrderByCreatedAtDesc(
                                savedEvent.getId()
                        )
                        .orElseThrow();


        assertEquals(
                SignalStatus.UNMAPPED,
                updatedEvent.getStatus()
        );


        assertEquals(
                JobStatus.COMPLETED,
                completedJob.getStatus()
        );


        assertEquals(
                AnalysisOutcomeType.UNMAPPED,
                savedAnalysis.getOutcomeType()
        );


        assertFalse(
                savedAnalysis.isMeaningfulChange()
        );


        assertNull(
                savedAnalysis.getTaskId()
        );
    }


    private UUID createAndClaimAnalyzeJob(
            UUID eventId
    ) {

        ProcessingJob savedJob =
                jobRepository.save(
                        new ProcessingJob(
                                eventId,
                                JobType.ANALYZE_EVENT,
                                1,
                                3
                        )
                );


        boolean claimed =
                claimService.claim(
                        savedJob.getId()
                );


        assertTrue(
                claimed
        );


        return savedJob.getId();
    }


    private CollaborationEvent createEvent(
            String content
    ) {

        return new CollaborationEvent(
                UUID.randomUUID().toString(),
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
}
