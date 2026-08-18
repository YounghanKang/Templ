package com.example.demo.job;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class AnalyzeJobProcessor {

    private final AnalyzeJobClaimService claimService;

    private final AnalyzeJobExecutionService executionService;

    private final ProcessingJobFailureService failureService;


    public boolean process(
            UUID jobId
    ) {

        if (jobId == null) {

            throw new IllegalArgumentException(
                    "jobId는 필수입니다."
            );
        }


        /*
         * 1. Job Claim
         *
         * READY / RETRY_WAIT
         *        ↓
         * PROCESSING
         *
         * 다른 Worker가 이미 처리하고 있거나
         * 처리할 수 없는 Job이면 false입니다.
         */
        boolean claimed =
                claimService.claim(
                        jobId
                );


        if (!claimed) {
            return false;
        }


        /*
         * 2. 실제 분석 실행
         */
        try {

            executionService.execute(
                    jobId
            );


            /*
             * execute() 내부 성공 흐름에서:
             *
             * AnalyzeEventCore
             * → EventAnalysis 저장
             * → Event 상태 변경
             * → Job COMPLETED
             *
             * 까지 처리됩니다.
             */
            return true;

        } catch (Exception exception) {

            /*
             * 3. 분석 실패
             *
             * attempt가 남아 있으면
             * → RETRY_WAIT
             *
             * 모두 사용했다면
             * → FAILED_FINAL
             */
            failureService.handle(
                    jobId,
                    exception
            );


            return false;
        }
    }
}