package com.example.demo.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.worker.analyze.enabled",
        havingValue = "true"
)
public class AnalyzeJobScheduler {

    private final AnalyzeJobBatchProcessor
            analyzeJobBatchProcessor;


    @Scheduled(
            fixedDelayString =
                    "${app.worker.analyze.fixed-delay-ms:5000}"
    )
    public void processAnalyzeJobs() {

        int processedCount =
                analyzeJobBatchProcessor
                        .processReadyJobs();


        if (processedCount > 0) {

            log.info(
                    "Analyze worker processed {} job(s).",
                    processedCount
            );
        }
    }
}