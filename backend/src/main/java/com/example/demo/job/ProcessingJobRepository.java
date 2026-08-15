package com.example.demo.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingJobRepository
        extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob>
    findTop10ByJobTypeAndStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            JobType jobType,
            List<JobStatus> statuses,
            Instant now
    );

    boolean existsByCollaborationEventIdAndJobTypeAndRunNumber(
            UUID collaborationEventId,
            JobType jobType,
            int runNumber
    );

    Optional<ProcessingJob>
    findByCollaborationEventIdAndJobTypeAndRunNumber(
            UUID collaborationEventId,
            JobType jobType,
            int runNumber
    );
}