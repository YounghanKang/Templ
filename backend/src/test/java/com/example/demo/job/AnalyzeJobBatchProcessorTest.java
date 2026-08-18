package com.example.demo.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AnalyzeJobBatchProcessorTest {

    private ProcessingJobRepository
            jobRepository;

    private AnalyzeJobProcessor
            analyzeJobProcessor;

    private AnalyzeJobBatchProcessor
            batchProcessor;


    @BeforeEach
    void setUp() {

        jobRepository =
                mock(
                        ProcessingJobRepository.class
                );

        analyzeJobProcessor =
                mock(
                        AnalyzeJobProcessor.class
                );


        batchProcessor =
                new AnalyzeJobBatchProcessor(
                        jobRepository,
                        analyzeJobProcessor
                );
    }


    @Test
    void readyAnalyzeJobsArePassedToProcessor() {

        ProcessingJob firstJob =
                mock(
                        ProcessingJob.class
                );

        ProcessingJob secondJob =
                mock(
                        ProcessingJob.class
                );


        UUID firstJobId =
                UUID.randomUUID();

        UUID secondJobId =
                UUID.randomUUID();


        when(
                firstJob.getId()
        ).thenReturn(
                firstJobId
        );


        when(
                secondJob.getId()
        ).thenReturn(
                secondJobId
        );


        when(
                jobRepository
                        .findTop10ByJobTypeAndStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                                eq(JobType.ANALYZE_EVENT),
                                eq(
                                        List.of(
                                                JobStatus.READY,
                                                JobStatus.RETRY_WAIT
                                        )
                                ),
                                any(Instant.class)
                        )
        ).thenReturn(
                List.of(
                        firstJob,
                        secondJob
                )
        );


        when(
                analyzeJobProcessor.process(
                        firstJobId
                )
        ).thenReturn(
                true
        );


        when(
                analyzeJobProcessor.process(
                        secondJobId
                )
        ).thenReturn(
                true
        );


        int processedCount =
                batchProcessor.processReadyJobs();


        assertEquals(
                2,
                processedCount
        );


        verify(
                analyzeJobProcessor,
                times(1)
        ).process(
                firstJobId
        );


        verify(
                analyzeJobProcessor,
                times(1)
        ).process(
                secondJobId
        );
    }


    @Test
    void failedProcessingIsNotIncludedInProcessedCount() {

        ProcessingJob firstJob =
                mock(
                        ProcessingJob.class
                );

        ProcessingJob secondJob =
                mock(
                        ProcessingJob.class
                );


        UUID firstJobId =
                UUID.randomUUID();

        UUID secondJobId =
                UUID.randomUUID();


        when(
                firstJob.getId()
        ).thenReturn(
                firstJobId
        );


        when(
                secondJob.getId()
        ).thenReturn(
                secondJobId
        );


        when(
                jobRepository
                        .findTop10ByJobTypeAndStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                                eq(JobType.ANALYZE_EVENT),
                                anyList(),
                                any(Instant.class)
                        )
        ).thenReturn(
                List.of(
                        firstJob,
                        secondJob
                )
        );


        when(
                analyzeJobProcessor.process(
                        firstJobId
                )
        ).thenReturn(
                true
        );


        when(
                analyzeJobProcessor.process(
                        secondJobId
                )
        ).thenReturn(
                false
        );


        int processedCount =
                batchProcessor.processReadyJobs();


        assertEquals(
                1,
                processedCount
        );
    }


    @Test
    void emptyQueueReturnsZero() {

        when(
                jobRepository
                        .findTop10ByJobTypeAndStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                                eq(JobType.ANALYZE_EVENT),
                                anyList(),
                                any(Instant.class)
                        )
        ).thenReturn(
                List.of()
        );


        int processedCount =
                batchProcessor.processReadyJobs();


        assertEquals(
                0,
                processedCount
        );


        verifyNoInteractions(
                analyzeJobProcessor
        );
    }
}