package com.example.demo.job;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "processing_job",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processing_job_event_type_run",
                        columnNames = {
                                "collaboration_event_id",
                                "job_type",
                                "run_number"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 분석할 CollaborationEvent ID.
     *
     * UUID만 저장
     * Job이 Event Entity 생명주기를 직접 소유하지 않도록
     */
    @Column(
            name = "collaboration_event_id",
            nullable = false
    )
    private UUID collaborationEventId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "job_type",
            nullable = false,
            length = 40
    )
    private JobType jobType;

    /**
     * 실행회차. 같은 Event를 재분석할 수 있으므로
     */
    @Column(
            name = "run_number",
            nullable = false
    )
    private int runNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private JobStatus status;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private int attemptCount;

    @Column(
            name = "max_attempts",
            nullable = false
    )
    private int maxAttempts;

    /**
     * Worker가 이 시각 이후에 Job을 가져갈 수 있음
     * Retry backoff에도 사용할 것
     */
    @Column(
            name = "available_at",
            nullable = false
    )
    private Instant availableAt;

    /**
     * Worker가 Job 가져간 시각.
     */
    @Column(name = "locked_at")
    private Instant lockedAt;

    /**
     * 어떤 Worker가 처리 중인지 식별값.
     */
    @Column(
            name = "locked_by",
            length = 100
    )
    private String lockedBy;

    @Column(
            name = "last_error_code",
            length = 100
    )
    private String lastErrorCode;

    @Column(
            name = "last_error_message",
            length = 1000
    )
    private String lastErrorMessage;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;


    public ProcessingJob(
            UUID collaborationEventId,
            JobType jobType,
            int runNumber,
            int maxAttempts
    ) {
        this.collaborationEventId =
                collaborationEventId;

        this.jobType =
                jobType;

        this.runNumber =
                runNumber;

        this.maxAttempts =
                maxAttempts;

        this.status =
                JobStatus.READY;

        this.attemptCount =
                0;

        this.availableAt =
                Instant.now();
    }


    @PrePersist
    protected void onCreate() {

        Instant now =
                Instant.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.availableAt == null) {
            this.availableAt = now;
        }

        if (this.status == null) {
            this.status =
                    JobStatus.READY;
        }

        if (this.runNumber <= 0) {
            this.runNumber = 1;
        }

        if (this.maxAttempts <= 0) {
            this.maxAttempts = 3;
        }
    }


    public void markProcessing(
            String workerId
    ) {
        this.status =
                JobStatus.PROCESSING;

        this.lockedAt =
                Instant.now();

        this.lockedBy =
                workerId;

        this.attemptCount++;
    }


    public void markCompleted() {

        this.status =
                JobStatus.COMPLETED;

        this.completedAt =
                Instant.now();

        this.lockedAt = null;
        this.lockedBy = null;

        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }


    public void markRetry(
            String errorCode,
            String errorMessage,
            Instant nextAvailableAt
    ) {

        this.status =
                JobStatus.RETRY_WAIT;

        this.lastErrorCode =
                errorCode;

        this.lastErrorMessage =
                errorMessage;

        this.availableAt =
                nextAvailableAt;

        this.lockedAt = null;
        this.lockedBy = null;
    }

    public void markFailedFinal(
            String errorCode,
            String errorMessage
    ) {

        this.status =
                JobStatus.FAILED_FINAL;

        this.lastErrorCode =
                errorCode;

        this.lastErrorMessage =
                errorMessage;

        this.lockedAt = null;
        this.lockedBy = null;
    }


    public boolean canRetry() {

        return this.attemptCount
                < this.maxAttempts;
    }
}