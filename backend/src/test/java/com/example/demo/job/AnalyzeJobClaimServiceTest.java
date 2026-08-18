package com.example.demo.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(AnalyzeJobClaimService.class)
class AnalyzeJobClaimServiceTest {

    @Autowired
    private AnalyzeJobClaimService claimService;

    @Autowired
    private ProcessingJobRepository jobRepository;


    @BeforeEach
    void cleanUp() {

        jobRepository.deleteAll();
    }


    @Test
    void readyAnalyzeJobCanBeClaimed() {

        ProcessingJob job =
                new ProcessingJob(
                        UUID.randomUUID(),
                        JobType.ANALYZE_EVENT,
                        1,
                        3
                );


        ProcessingJob savedJob =
                jobRepository.save(job);


        boolean claimed =
                claimService.claim(
                        savedJob.getId()
                );


        assertTrue(
                claimed
        );


        ProcessingJob claimedJob =
                jobRepository
                        .findById(
                                savedJob.getId()
                        )
                        .orElseThrow();


        assertEquals(
                JobStatus.PROCESSING,
                claimedJob.getStatus()
        );


        assertEquals(
                1,
                claimedJob.getAttemptCount()
        );
    }


    @Test
    void filterJobCannotBeClaimedByAnalyzeWorker() {

        ProcessingJob job =
                new ProcessingJob(
                        UUID.randomUUID(),
                        JobType.FILTER_EVENT,
                        1,
                        3
                );


        ProcessingJob savedJob =
                jobRepository.save(job);


        boolean claimed =
                claimService.claim(
                        savedJob.getId()
                );


        assertFalse(
                claimed
        );


        ProcessingJob unchangedJob =
                jobRepository
                        .findById(
                                savedJob.getId()
                        )
                        .orElseThrow();


        assertEquals(
                JobStatus.READY,
                unchangedJob.getStatus()
        );


        assertEquals(
                0,
                unchangedJob.getAttemptCount()
        );
    }


    @Test
    void nonexistentJobCannotBeClaimed() {

        boolean claimed =
                claimService.claim(
                        UUID.randomUUID()
                );


        assertFalse(
                claimed
        );
    }
}