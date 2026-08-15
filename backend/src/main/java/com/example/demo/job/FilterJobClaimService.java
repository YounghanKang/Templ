package com.example.demo.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FilterJobClaimService {

    private static final String WORKER_ID =
            "filter-worker";

    private final ProcessingJobRepository jobRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(UUID jobId) {

        ProcessingJob job =
                jobRepository
                        .findById(jobId)
                        .orElse(null);


        if (job == null) {
            return false;
        }


        if (job.getJobType() != JobType.FILTER_EVENT) {
            return false;
        }


        if (
                job.getStatus() != JobStatus.READY
                        && job.getStatus() != JobStatus.RETRY_WAIT
        ) {
            return false;
        }


        if (job.getAvailableAt().isAfter(Instant.now())) {
            return false;
        }


        job.markProcessing(
                WORKER_ID
        );


        return true;
    }
}