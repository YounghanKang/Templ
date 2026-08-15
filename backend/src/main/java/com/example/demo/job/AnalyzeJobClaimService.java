package com.example.demo.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyzeJobClaimService {

    private static final String WORKER_ID =
            "analyze-worker";

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


        /*
         * Analyze Worker는
         * ANALYZE_EVENT Job만 처리합니다.
         */
        if (job.getJobType() != JobType.ANALYZE_EVENT) {
            return false;
        }


        /*
         * READY 또는 재시도 대기 상태만
         * 가져올 수 있습니다.
         */
        if (
                job.getStatus() != JobStatus.READY
                        && job.getStatus() != JobStatus.RETRY_WAIT
        ) {
            return false;
        }


        /*
         * RETRY_WAIT의 경우
         * 재실행 시간이 아직 오지 않았다면
         * 가져가지 않습니다.
         */
        if (job.getAvailableAt().isAfter(Instant.now())) {
            return false;
        }


        /*
         * 기존 ProcessingJob의 markProcessing()
         * 메서드를 이용합니다.
         *
         * 여기서:
         * READY → PROCESSING
         * attemptCount + 1
         * lockedBy 설정
         * lockedAt 설정
         */
        job.markProcessing(
                WORKER_ID
        );


        return true;
    }
}