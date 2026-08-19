package com.example.demo.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AnalyzeJobBatchProcessor {

    private static final List<JobStatus> PROCESSABLE_STATUSES =
            List.of(
                    JobStatus.READY,
                    JobStatus.RETRY_WAIT
            );


    private final ProcessingJobRepository jobRepository;

    private final AnalyzeJobProcessor analyzeJobProcessor;


    public int processReadyJobs() {

        /*
         * 1. 지금 처리 가능한 ANALYZE_EVENT Job 조회
         *
         * - READY
         * - RETRY_WAIT
         * - availableAt <= 현재 시간
         *
         * 최대 10개만 가져옵니다.
         */
        List<ProcessingJob> jobs =
                jobRepository
                        .findTop10ByJobTypeAndStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                                JobType.ANALYZE_EVENT,
                                PROCESSABLE_STATUSES,
                                Instant.now()
                        );


        int processedCount = 0;


        /*
         * 2. 각각 AnalyzeJobProcessor에 전달
         */
        for (ProcessingJob job : jobs) {

            boolean processed =
                    analyzeJobProcessor.process(
                            job.getId()
                    );


            if (processed) {
                processedCount++;
            }
        }


        return processedCount;
    }
}