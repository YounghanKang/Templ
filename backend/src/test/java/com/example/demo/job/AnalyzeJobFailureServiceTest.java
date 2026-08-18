package com.example.demo.job;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import com.example.demo.signal.SignalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        AnalyzeJobClaimService.class,
        ProcessingJobFailureService.class
})
class AnalyzeJobFailureServiceTest {

    @Autowired
    private AnalyzeJobClaimService claimService;

    @Autowired
    private ProcessingJobFailureService failureService;

    @Autowired
    private CollaborationEventRepository eventRepository;

    @Autowired
    private ProcessingJobRepository jobRepository;


    @BeforeEach
    void cleanUp() {

        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }


    @Test
    void firstAnalyzeFailureMovesJobToRetryWait() {

        CollaborationEvent savedEvent =
                eventRepository.save(
                        createEvent(
                                "로그인 API를 삭제합니다."
                        )
                );


        ProcessingJob savedJob =
                jobRepository.save(
                        new ProcessingJob(
                                savedEvent.getId(),
                                JobType.ANALYZE_EVENT,
                                1,
                                3
                        )
                );


        boolean claimed =
                claimService.claim(
                        savedJob.getId()
                );


        assertEquals(
                true,
                claimed
        );


        failureService.handle(
                savedJob.getId(),
                new RuntimeException(
                        "AI API 일시 오류"
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
    void lastAnalyzeFailureMovesJobToFailedFinal() {

        CollaborationEvent savedEvent =
                eventRepository.save(
                        createEvent(
                                "로그인 API를 삭제합니다."
                        )
                );


        ProcessingJob savedJob =
                jobRepository.save(
                        new ProcessingJob(
                                savedEvent.getId(),
                                JobType.ANALYZE_EVENT,
                                1,
                                1
                        )
                );


        boolean claimed =
                claimService.claim(
                        savedJob.getId()
                );


        assertEquals(
                true,
                claimed
        );


        failureService.handle(
                savedJob.getId(),
                new RuntimeException(
                        "AI API 최종 오류"
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
