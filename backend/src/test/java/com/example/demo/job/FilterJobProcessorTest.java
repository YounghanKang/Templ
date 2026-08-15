package com.example.demo.job;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import com.example.demo.filter.EventFilterApplicationService;
import com.example.demo.filter.EventFilterResultRepository;
import com.example.demo.filter.RuleBasedEventFilter;
import com.example.demo.signal.SignalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        RuleBasedEventFilter.class,
        EventFilterApplicationService.class,
        FilterJobClaimService.class,
        FilterJobExecutionService.class,
        ProcessingJobFailureService.class,
        FilterJobProcessor.class
})
class FilterJobProcessorTest {

    @Autowired
    private FilterJobProcessor filterJobProcessor;

    @Autowired
    private CollaborationEventRepository eventRepository;

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private EventFilterResultRepository filterResultRepository;

    @Autowired
    private FilterJobClaimService claimService;

    @Autowired
    private ProcessingJobFailureService failureService;

    @BeforeEach
    void cleanUp() {

        filterResultRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void importantMessageCreatesAnalyzeJob() {

        CollaborationEvent event =
                createEvent(
                        "API 삭제",
                        false
                );

        CollaborationEvent savedEvent =
                eventRepository.save(event);


        ProcessingJob filterJob =
                new ProcessingJob(
                        savedEvent.getId(),
                        JobType.FILTER_EVENT,
                        1,
                        3
                );

        jobRepository.save(filterJob);


        int processed =
                filterJobProcessor
                        .processReadyJobs();


        assertEquals(
                1,
                processed
        );


        ProcessingJob completedFilterJob =
                jobRepository
                        .findById(
                                filterJob.getId()
                        )
                        .orElseThrow();

        assertEquals(
                JobStatus.COMPLETED,
                completedFilterJob.getStatus()
        );


        ProcessingJob analyzeJob =
                jobRepository
                        .findByCollaborationEventIdAndJobTypeAndRunNumber(
                                savedEvent.getId(),
                                JobType.ANALYZE_EVENT,
                                1
                        )
                        .orElseThrow();

        assertEquals(
                JobStatus.READY,
                analyzeJob.getStatus()
        );


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(
                                savedEvent.getId()
                        )
                        .orElseThrow();

        assertEquals(
                SignalStatus.AI_PENDING,
                updatedEvent.getStatus()
        );


        assertTrue(
                filterResultRepository
                        .findTopByCollaborationEventIdOrderByCreatedAtDesc(
                                savedEvent.getId()
                        )
                        .isPresent()
        );
    }


    @Test
    void trivialMessageDoesNotCreateAnalyzeJob() {

        CollaborationEvent event =
                createEvent(
                        "감사합니다",
                        false
                );

        CollaborationEvent savedEvent =
                eventRepository.save(event);


        ProcessingJob filterJob =
                new ProcessingJob(
                        savedEvent.getId(),
                        JobType.FILTER_EVENT,
                        1,
                        3
                );

        jobRepository.save(filterJob);


        int processed =
                filterJobProcessor
                        .processReadyJobs();


        assertEquals(
                1,
                processed
        );


        ProcessingJob completedFilterJob =
                jobRepository
                        .findById(
                                filterJob.getId()
                        )
                        .orElseThrow();

        assertEquals(
                JobStatus.COMPLETED,
                completedFilterJob.getStatus()
        );


        boolean analyzeJobExists =
                jobRepository
                        .existsByCollaborationEventIdAndJobTypeAndRunNumber(
                                savedEvent.getId(),
                                JobType.ANALYZE_EVENT,
                                1
                        );

        assertFalse(
                analyzeJobExists
        );


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(
                                savedEvent.getId()
                        )
                        .orElseThrow();

        assertEquals(
                SignalStatus.FILTERED_OUT,
                updatedEvent.getStatus()
        );
    }


    private CollaborationEvent createEvent(
            String content,
            boolean generatedBySystem
    ) {

        return new CollaborationEvent(
                UUID.randomUUID(),
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
                generatedBySystem,
                Instant.now()
        );
    }
    @Test
    void firstFailureMovesJobToRetryWait() {

        CollaborationEvent event =
                createEvent(
                        "API 삭제",
                        false
                );

        CollaborationEvent savedEvent =
                eventRepository.save(event);


        ProcessingJob filterJob =
                new ProcessingJob(
                        savedEvent.getId(),
                        JobType.FILTER_EVENT,
                        1,
                        3
                );

        ProcessingJob savedJob =
                jobRepository.save(filterJob);


        boolean claimed =
                claimService.claim(
                        savedJob.getId()
                );

        assertTrue(claimed);


        failureService.handle(
                savedJob.getId(),
                new RuntimeException(
                        "테스트용 일시적 오류"
                )
        );


        ProcessingJob failedJob =
                jobRepository
                        .findById(
                                savedJob.getId()
                        )
                        .orElseThrow();


        assertEquals(
                JobStatus.RETRY_WAIT,
                failedJob.getStatus()
        );

        assertEquals(
                1,
                failedJob.getAttemptCount()
        );

        assertEquals(
                "RuntimeException",
                failedJob.getLastErrorCode()
        );

        assertNotNull(
                failedJob.getAvailableAt()
        );


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(
                                savedEvent.getId()
                        )
                        .orElseThrow();


        assertEquals(
                SignalStatus.FAILED_RETRYABLE,
                updatedEvent.getStatus()
        );
    }
    @Test
    void lastAllowedFailureMovesJobToFailedFinal() {

        CollaborationEvent event =
                createEvent(
                        "API 삭제",
                        false
                );

        CollaborationEvent savedEvent =
                eventRepository.save(event);


        ProcessingJob filterJob =
                new ProcessingJob(
                        savedEvent.getId(),
                        JobType.FILTER_EVENT,
                        1,
                        1
                );

        ProcessingJob savedJob =
                jobRepository.save(filterJob);


        boolean claimed =
                claimService.claim(
                        savedJob.getId()
                );

        assertTrue(claimed);


        failureService.handle(
                savedJob.getId(),
                new RuntimeException(
                        "테스트용 최종 오류"
                )
        );


        ProcessingJob failedJob =
                jobRepository
                        .findById(
                                savedJob.getId()
                        )
                        .orElseThrow();


        assertEquals(
                JobStatus.FAILED_FINAL,
                failedJob.getStatus()
        );

        assertEquals(
                1,
                failedJob.getAttemptCount()
        );

        assertEquals(
                "RuntimeException",
                failedJob.getLastErrorCode()
        );


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(
                                savedEvent.getId()
                        )
                        .orElseThrow();


        assertEquals(
                SignalStatus.FAILED_FINAL,
                updatedEvent.getStatus()
        );
    }
}