package com.example.demo.job;

import com.example.demo.collaboration.CollaborationEventRepository;
import com.example.demo.signal.SignalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessingJobFailureService {

    private static final long RETRY_DELAY_SECONDS =
            30;

    private final ProcessingJobRepository jobRepository;

    private final CollaborationEventRepository
            eventRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(
            UUID jobId,
            Exception exception
    ) {

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


        String errorCode =
                exception
                        .getClass()
                        .getSimpleName();


        String errorMessage =
                normalizeErrorMessage(
                        exception
                );


        if (job.canRetry()) {

            job.markRetry(
                    errorCode,
                    errorMessage,
                    Instant.now()
                            .plusSeconds(
                                    RETRY_DELAY_SECONDS
                            )
            );


            eventRepository
                    .findById(
                            job.getCollaborationEventId()
                    )
                    .ifPresent(
                            event ->
                                    event.changeStatus(
                                            SignalStatus.FAILED_RETRYABLE
                                    )
                    );


            return;
        }


        job.markFailedFinal(
                errorCode,
                errorMessage
        );


        eventRepository
                .findById(
                        job.getCollaborationEventId()
                )
                .ifPresent(
                        event ->
                                event.changeStatus(
                                        SignalStatus.FAILED_FINAL
                                )
                );
    }


    private String normalizeErrorMessage(
            Exception exception
    ) {

        String message =
                exception.getMessage();


        if (
                message == null
                        || message.isBlank()
        ) {

            message =
                    exception
                            .getClass()
                            .getSimpleName();
        }


        if (message.length() > 1000) {

            return message.substring(
                    0,
                    1000
            );
        }


        return message;
    }
}