package com.example.demo.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilterJobScheduler {

    private final FilterJobProcessor filterJobProcessor;


    @Scheduled(
            fixedDelayString =
                    "${app.worker.filter.fixed-delay-ms:5000}"
    )
    public void processFilterJobs() {

        int processedCount =
                filterJobProcessor.processReadyJobs();


        if (processedCount > 0) {

            log.info(
                    "Filter worker processed {} job(s).",
                    processedCount
            );
        }
    }
}