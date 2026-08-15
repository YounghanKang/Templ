package com.example.demo.collaboration;

import com.example.demo.job.JobType;
import com.example.demo.job.ProcessingJob;
import com.example.demo.job.ProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollaborationEventIngestionService {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final CollaborationEventRepository eventRepository;
    private final ProcessingJobRepository jobRepository;

    @Transactional
    public IngestionResult ingest(
            UUID projectId,
            SourceTool sourceTool,
            CollaborationEventType eventType,
            EventAction eventAction,
            String externalSourceId,
            String title,
            String content,
            String actorId,
            String actorName,
            String sourceLocation,
            String sourceUrl,
            boolean generatedBySystem,
            Instant occurredAt
    ) {

        boolean duplicated =
                externalSourceId != null
                        && eventRepository
                        .existsByProjectIdAndSourceToolAndExternalSourceId(
                                projectId,
                                sourceTool,
                                externalSourceId
                        );

        if (duplicated) {
            return IngestionResult.duplicated();
        }

        CollaborationEvent event =
                new CollaborationEvent(
                        projectId,
                        sourceTool,
                        eventType,
                        eventAction,
                        externalSourceId,
                        title,
                        content,
                        actorId,
                        actorName,
                        sourceLocation,
                        sourceUrl,
                        generatedBySystem,
                        occurredAt
                );

        CollaborationEvent savedEvent =
                eventRepository.save(event);

        ProcessingJob job =
                new ProcessingJob(
                        savedEvent.getId(),
                        JobType.FILTER_EVENT,
                        1,
                        DEFAULT_MAX_ATTEMPTS
                );

        ProcessingJob savedJob =
                jobRepository.save(job);

        return IngestionResult.created(
                savedEvent.getId(),
                savedJob.getId()
        );
    }


    public record IngestionResult(
            UUID eventId,
            UUID jobId,
            boolean duplicate
    ) {

        public static IngestionResult created(
                UUID eventId,
                UUID jobId
        ) {
            return new IngestionResult(
                    eventId,
                    jobId,
                    false
            );
        }

        public static IngestionResult duplicated() {
            return new IngestionResult(
                    null,
                    null,
                    true
            );
        }
    }
}