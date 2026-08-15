package com.example.demo.job;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import com.example.demo.filter.EventFilterResultRepository;
import com.example.demo.signal.SignalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(
        properties = {
                "app.worker.filter.fixed-delay-ms=100"
        }
)
class FilterJobSchedulerIntegrationTest {

    @Autowired
    private CollaborationEventRepository eventRepository;

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private EventFilterResultRepository filterResultRepository;


    @BeforeEach
    void cleanUp() {

        filterResultRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }


    @Test
    void readyFilterJobIsProcessedAutomatically()
            throws InterruptedException {

        /*
         * 1. 테스트용 CollaborationEvent 생성
         *
         * "감사합니다"는 RuleBasedEventFilter에서
         * FILTERED_OUT으로 분류되어야 합니다.
         */
        CollaborationEvent event =
                new CollaborationEvent(
                        UUID.randomUUID(),
                        SourceTool.SLACK,
                        CollaborationEventType.MESSAGE,
                        EventAction.CREATED,
                        "Ev-scheduler-test-001",
                        null,
                        "감사합니다",
                        "U001",
                        "테스트 사용자",
                        "#backend",
                        null,
                        false,
                        Instant.now()
                );


        CollaborationEvent savedEvent =
                eventRepository.save(event);


        /*
         * 2. FILTER_EVENT / READY Job 생성
         */
        ProcessingJob filterJob =
                new ProcessingJob(
                        savedEvent.getId(),
                        JobType.FILTER_EVENT,
                        1,
                        3
                );


        ProcessingJob savedJob =
                jobRepository.save(filterJob);


        /*
         * 여기서는 일부러
         *
         * filterJobProcessor.processReadyJobs();
         *
         * 를 호출하지 않습니다.
         *
         * @Scheduled가 자동으로 실행해야 합니다.
         */


        /*
         * 3. Scheduler가 Job을 처리할 시간을 기다립니다.
         *
         * 테스트에서는 fixed-delay를 100ms로 줄였습니다.
         * 최대 3초까지만 기다립니다.
         */
        long deadline =
                System.currentTimeMillis()
                        + 3000;


        JobStatus finalStatus =
                JobStatus.READY;


        while (
                System.currentTimeMillis()
                        < deadline
        ) {

            ProcessingJob currentJob =
                    jobRepository
                            .findById(
                                    savedJob.getId()
                            )
                            .orElseThrow();


            finalStatus =
                    currentJob.getStatus();


            if (
                    finalStatus
                            == JobStatus.COMPLETED
            ) {
                break;
            }


            Thread.sleep(50);
        }


        /*
         * 4. Filter Job이 자동으로 완료됐는지 확인
         */
        assertEquals(
                JobStatus.COMPLETED,
                finalStatus
        );


        /*
         * 5. CollaborationEvent 상태도
         * FILTERED_OUT으로 변경됐는지 확인
         */
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


        /*
         * 6. 사소한 메시지이므로
         * ANALYZE_EVENT Job은 없어야 합니다.
         */
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
    }
}