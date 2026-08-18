package com.example.demo.job;

import com.example.demo.analysis.AnalyzeExecutionOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AnalyzeJobProcessorTest {

    private AnalyzeJobClaimService
            claimService;

    private AnalyzeJobExecutionService
            executionService;

    private ProcessingJobFailureService
            failureService;

    private AnalyzeJobProcessor
            processor;


    @BeforeEach
    void setUp() {

        claimService =
                mock(
                        AnalyzeJobClaimService.class
                );

        executionService =
                mock(
                        AnalyzeJobExecutionService.class
                );

        failureService =
                mock(
                        ProcessingJobFailureService.class
                );


        processor =
                new AnalyzeJobProcessor(
                        claimService,
                        executionService,
                        failureService
                );
    }


    @Test
    void claimedJobIsExecutedSuccessfully() {

        UUID jobId =
                UUID.randomUUID();


        when(
                claimService.claim(jobId)
        ).thenReturn(
                true
        );


        when(
                executionService.execute(jobId)
        ).thenReturn(
                mock(
                        AnalyzeExecutionOutcome.class
                )
        );


        boolean processed =
                processor.process(
                        jobId
                );


        assertTrue(
                processed
        );


        verify(
                claimService,
                times(1)
        ).claim(
                jobId
        );


        verify(
                executionService,
                times(1)
        ).execute(
                jobId
        );


        verifyNoInteractions(
                failureService
        );
    }


    @Test
    void unclaimableJobIsNotExecuted() {

        UUID jobId =
                UUID.randomUUID();


        when(
                claimService.claim(jobId)
        ).thenReturn(
                false
        );


        boolean processed =
                processor.process(
                        jobId
                );


        assertFalse(
                processed
        );


        verify(
                claimService,
                times(1)
        ).claim(
                jobId
        );


        verifyNoInteractions(
                executionService
        );


        verifyNoInteractions(
                failureService
        );
    }


    @Test
    void executionFailureIsHandledByFailureService() {

        UUID jobId =
                UUID.randomUUID();


        RuntimeException exception =
                new RuntimeException(
                        "AI API 일시 오류"
                );


        when(
                claimService.claim(jobId)
        ).thenReturn(
                true
        );


        when(
                executionService.execute(jobId)
        ).thenThrow(
                exception
        );


        boolean processed =
                processor.process(
                        jobId
                );


        assertFalse(
                processed
        );


        verify(
                claimService,
                times(1)
        ).claim(
                jobId
        );


        verify(
                executionService,
                times(1)
        ).execute(
                jobId
        );


        verify(
                failureService,
                times(1)
        ).handle(
                jobId,
                exception
        );
    }


    @Test
    void nullJobIdIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        processor.process(
                                null
                        )
        );


        verifyNoInteractions(
                claimService
        );


        verifyNoInteractions(
                executionService
        );


        verifyNoInteractions(
                failureService
        );
    }
}