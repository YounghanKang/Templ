package com.example.demo.job;

import com.example.demo.analysis.AnalysisOutcomeType;
import com.example.demo.analysis.AnalyzeExecutionOutcome;
import com.example.demo.analysis.ContextAnalysisResult;
import com.example.demo.analysis.EventAnalysis;
import com.example.demo.analysis.EventAnalysisRepository;
import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import com.example.demo.signal.SignalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyzeJobCompletionService {

    private static final String PROMPT_VERSION =
            "v1";

    private static final String SCHEMA_VERSION =
            "v1";


    private final ProcessingJobRepository jobRepository;

    private final CollaborationEventRepository eventRepository;

    private final EventAnalysisRepository analysisRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(
            UUID jobId,
            AnalyzeExecutionOutcome outcome
    ) {

        if (jobId == null) {

            throw new IllegalArgumentException(
                    "jobId는 필수입니다."
            );
        }


        if (outcome == null) {

            throw new IllegalArgumentException(
                    "outcome은 필수입니다."
            );
        }


        if (outcome.outcomeType() == null) {

            throw new IllegalArgumentException(
                    "outcomeType은 필수입니다."
            );
        }


        ProcessingJob job =
                jobRepository
                        .findById(jobId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "ProcessingJob을 찾을 수 없습니다."
                                        )
                        );


        if (
                job.getJobType()
                        != JobType.ANALYZE_EVENT
        ) {

            throw new IllegalStateException(
                    "ANALYZE_EVENT Job만 완료할 수 있습니다."
            );
        }


        if (
                job.getStatus()
                        != JobStatus.PROCESSING
        ) {

            throw new IllegalStateException(
                    "PROCESSING 상태의 Job만 완료할 수 있습니다."
            );
        }


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
         * 같은 Event + runNumber의 분석 결과가
         * 이미 존재하면 중복 완료를 막습니다.
         */
        boolean duplicated =
                analysisRepository
                        .existsByCollaborationEventIdAndRunNumber(
                                event.getId(),
                                job.getRunNumber()
                        );


        if (duplicated) {

            throw new IllegalStateException(
                    "이미 저장된 분석 실행 결과입니다."
            );
        }


        /*
         * 1. EventAnalysis 생성
         */
        EventAnalysis eventAnalysis =
                createEventAnalysis(
                        event,
                        job,
                        outcome
                );


        /*
         * 2. 분석 결과 저장
         */
        analysisRepository.save(
                eventAnalysis
        );


        /*
         * 3. CollaborationEvent 상태 변경
         */
        SignalStatus signalStatus =
                determineSignalStatus(
                        outcome.outcomeType()
                );


        event.changeStatus(
                signalStatus
        );


        /*
         * 4. ProcessingJob 완료
         */
        job.markCompleted();


        /*
         * 명시적으로 저장합니다.
         */
        eventRepository.save(
                event
        );


        jobRepository.save(
                job
        );
    }


    private EventAnalysis createEventAnalysis(
            CollaborationEvent event,
            ProcessingJob job,
            AnalyzeExecutionOutcome outcome
    ) {

        /*
         * UNMAPPED에서는 AI를 호출하지 않았기 때문에
         * analysisResult가 null입니다.
         */
        if (
                outcome.outcomeType()
                        == AnalysisOutcomeType.UNMAPPED
        ) {

            return new EventAnalysis(
                    event.getId(),
                    job.getRunNumber(),
                    AnalysisOutcomeType.UNMAPPED,
                    false,
                    null,
                    "관련 프로젝트 Node를 찾지 못했습니다.",
                    1,
                    0.0,
                    null,
                    null,
                    PROMPT_VERSION,
                    SCHEMA_VERSION
            );
        }


        ContextAnalysisResult result =
                outcome.analysisResult();


        if (result == null) {

            throw new IllegalStateException(
                    "분석 완료 결과에는 analysisResult가 필요합니다."
            );
        }


        return new EventAnalysis(
                event.getId(),
                job.getRunNumber(),
                outcome.outcomeType(),
                result.meaningfulChange(),
                result.changeType(),
                result.summary(),
                result.riskScore(),
                result.confidence(),
                result.taskId(),
                null,
                PROMPT_VERSION,
                SCHEMA_VERSION
        );
    }


    private SignalStatus determineSignalStatus(
            AnalysisOutcomeType outcomeType
    ) {

        return switch (outcomeType) {

            case UNMAPPED ->
                    SignalStatus.UNMAPPED;

            case NO_WARNING ->
                    SignalStatus.ANALYZED;

            case SOFT_WARNING,
                 HARD_WARNING ->
                    SignalStatus.WARNING_CREATED;
        };
    }
}