package com.example.demo.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FilterJobProcessor {

    private final ProcessingJobRepository jobRepository;

    private final FilterJobClaimService claimService;

    private final FilterJobExecutionService executionService;

    private final ProcessingJobFailureService failureService;


    public int processReadyJobs() {

        List<ProcessingJob> jobs =
                jobRepository
                        .findTop10ByJobTypeAndStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                                JobType.FILTER_EVENT,
                                List.of(
                                        JobStatus.READY,
                                        JobStatus.RETRY_WAIT
                                ),
                                Instant.now()
                        );


        int processedCount = 0;


        for (ProcessingJob candidate : jobs) {

            boolean claimed =
                    claimService.claim(
                            candidate.getId()
                    );


            if (!claimed) {
                continue;
            }


            try {

                executionService.execute(
                        candidate.getId()
                );


            } catch (Exception exception) {

                failureService.handle(
                        candidate.getId(),
                        exception
                );
            }


            processedCount++;
        }


        return processedCount;
    }
}