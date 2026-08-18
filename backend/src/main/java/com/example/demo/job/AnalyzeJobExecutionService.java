package com.example.demo.job;

import com.example.demo.analysis.AnalyzeEventCore;
import com.example.demo.analysis.AnalyzeExecutionOutcome;
import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class AnalyzeJobExecutionService {

    private final ProcessingJobRepository jobRepository;

    private final CollaborationEventRepository eventRepository;

    private final AnalyzeEventCore analyzeEventCore;

    private final AnalyzeJobCompletionService completionService;


    public AnalyzeExecutionOutcome execute(
            UUID jobId
    ) {

        if (jobId == null) {

            throw new IllegalArgumentException(
                    "jobId는 필수입니다."
            );
        }


        /*
         * 1. Job 조회
         */
        ProcessingJob job =
                jobRepository
                        .findById(jobId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "ProcessingJob을 찾을 수 없습니다."
                                        )
                        );


        /*
         * 2. ANALYZE_EVENT인지 확인
         */
        if (
                job.getJobType()
                        != JobType.ANALYZE_EVENT
        ) {

            throw new IllegalStateException(
                    "ANALYZE_EVENT Job만 실행할 수 있습니다."
            );
        }


        /*
         * 3. Claim이 완료된 PROCESSING 상태인지 확인
         */
        if (
                job.getStatus()
                        != JobStatus.PROCESSING
        ) {

            throw new IllegalStateException(
                    "PROCESSING 상태의 Job만 실행할 수 있습니다."
            );
        }


        /*
         * 4. 원본 Event 조회
         */
        CollaborationEvent event =
                eventRepository
                        .findById(
                                job.getCollaborationEventId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "CollaborationEvent를 찾을 수 없습니다."
                                        )
                        );


        /*
         * 5. 분석 실행
         *
         * 이 부분에서 나중에 OpenAI 호출까지 발생합니다.
         * DB 완료 트랜잭션 밖에서 실행합니다.
         */
        AnalyzeExecutionOutcome outcome =
                analyzeEventCore.execute(
                        event
                );


        /*
         * 6. 분석 성공 결과를
         * 별도 트랜잭션에서 원자적으로 확정
         */
        completionService.complete(
                jobId,
                outcome
        );


        return outcome;
    }
}