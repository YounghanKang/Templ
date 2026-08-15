package com.example.demo.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AnalyzeJobSchedulerTest {

    private AnalyzeJobBatchProcessor
            batchProcessor;

    private AnalyzeJobScheduler
            scheduler;


    @BeforeEach
    void setUp() {

        batchProcessor =
                mock(
                        AnalyzeJobBatchProcessor.class
                );


        scheduler =
                new AnalyzeJobScheduler(
                        batchProcessor
                );
    }


    @Test
    void schedulerDelegatesToBatchProcessor() {

        when(
                batchProcessor.processReadyJobs()
        ).thenReturn(
                2
        );


        scheduler.processAnalyzeJobs();


        verify(
                batchProcessor,
                times(1)
        ).processReadyJobs();
    }


    @Test
    void zeroJobsIsAlsoHandledNormally() {

        when(
                batchProcessor.processReadyJobs()
        ).thenReturn(
                0
        );


        scheduler.processAnalyzeJobs();


        verify(
                batchProcessor,
                times(1)
        ).processReadyJobs();
    }
}