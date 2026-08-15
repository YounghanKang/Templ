package com.example.demo.job;

import com.example.demo.analysis.AnalysisOutcomeType;
import com.example.demo.analysis.AnalyzeEventCore;
import com.example.demo.analysis.AnalyzeExecutionOutcome;
import com.example.demo.analysis.ChangeType;
import com.example.demo.analysis.ContextAnalysisResult;
import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import com.example.demo.matching.NodeMatchingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AnalyzeJobExecutionServiceTest {

    private ProcessingJobRepository
            jobRepository;

    private CollaborationEventRepository
            eventRepository;

    private AnalyzeEventCore
            analyzeEventCore;

    private AnalyzeJobCompletionService
            completionService;

    private AnalyzeJobExecutionService
            executionService;


    @BeforeEach
    void setUp() {

        jobRepository =
                mock(
                        ProcessingJobRepository.class
                );

        eventRepository =
                mock(
                        CollaborationEventRepository.class
                );

        analyzeEventCore =
                mock(
                        AnalyzeEventCore.class
                );

        completionService =
                mock(
                        AnalyzeJobCompletionService.class
                );


        executionService =
                new AnalyzeJobExecutionService(
                        jobRepository,
                        eventRepository,
                        analyzeEventCore,
                        completionService
                );
    }


    @Test
    void processedAnalyzeJobExecutesCoreAndCompletesResult() {

        UUID jobId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        UUID projectId =
                UUID.randomUUID();

        UUID nodeId =
                UUID.randomUUID();


        ProcessingJob job =
                mock(
                        ProcessingJob.class
                );


        CollaborationEvent event =
                mock(
                        CollaborationEvent.class
                );


        when(
                jobRepository.findById(jobId)
        ).thenReturn(
                Optional.of(job)
        );


        when(
                job.getJobType()
        ).thenReturn(
                JobType.ANALYZE_EVENT
        );


        when(
                job.getStatus()
        ).thenReturn(
                JobStatus.PROCESSING
        );


        when(
                job.getCollaborationEventId()
        ).thenReturn(
                eventId
        );


        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Optional.of(event)
        );


        ContextAnalysisResult analysisResult =
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


        AnalyzeExecutionOutcome expectedOutcome =
                AnalyzeExecutionOutcome.analyzed(
                        AnalysisOutcomeType.HARD_WARNING,
                        NodeMatchingResult.mapped(
                                projectId,
                                "#backend",
                                List.of()
                        ),
                        null,
                        analysisResult
                );


        when(
                analyzeEventCore.execute(
                        event
                )
        ).thenReturn(
                expectedOutcome
        );


        AnalyzeExecutionOutcome actualOutcome =
                executionService.execute(
                        jobId
                );


        assertEquals(
                expectedOutcome,
                actualOutcome
        );


        verify(
                analyzeEventCore,
                times(1)
        ).execute(
                event
        );


        verify(
                completionService,
                times(1)
        ).complete(
                jobId,
                expectedOutcome
        );
    }
}