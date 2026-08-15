package com.example.demo.job;

import com.example.demo.filter.EventFilterApplicationService;
import com.example.demo.filter.FilterDecision;
import com.example.demo.filter.FilterResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FilterJobExecutionService {

    private static final int ANALYSIS_MAX_ATTEMPTS =
            3;

    private final ProcessingJobRepository jobRepository;

    private final EventFilterApplicationService
            filterApplicationService;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(UUID jobId) {

        ProcessingJob job =
                jobRepository
                        .findById(jobId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "ProcessingJob not found: "
                                                        + jobId
                                        )
                        );


        if (job.getJobType() != JobType.FILTER_EVENT) {

            throw new IllegalStateException(
                    "FILTER_EVENT Job이 아닙니다."
            );
        }


        if (job.getStatus() != JobStatus.PROCESSING) {

            throw new IllegalStateException(
                    "PROCESSING 상태의 Job만 실행할 수 있습니다."
            );
        }


        FilterResult result =
                filterApplicationService.filter(
                        job.getCollaborationEventId()
                );


        if (
                result.decision()
                        == FilterDecision.AI_REQUIRED
        ) {

            createAnalyzeJobIfAbsent(
                    job.getCollaborationEventId()
            );
        }


        job.markCompleted();
    }


    private void createAnalyzeJobIfAbsent(
            UUID collaborationEventId
    ) {

        boolean alreadyExists =
                jobRepository
                        .existsByCollaborationEventIdAndJobTypeAndRunNumber(
                                collaborationEventId,
                                JobType.ANALYZE_EVENT,
                                1
                        );


        if (alreadyExists) {
            return;
        }


        ProcessingJob analyzeJob =
                new ProcessingJob(
                        collaborationEventId,
                        JobType.ANALYZE_EVENT,
                        1,
                        ANALYSIS_MAX_ATTEMPTS
                );


        jobRepository.save(
                analyzeJob
        );
    }
}