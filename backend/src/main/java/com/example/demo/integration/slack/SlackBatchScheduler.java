package com.example.demo.integration.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackBatchScheduler {

    private final SlackMessageBatchService batchService;

    private final SlackBatchIngestionService batchIngestionService;


    @Scheduled(
            fixedDelayString =
                    "${app.worker.slack-batch.fixed-delay-ms:5000}"
    )
    public void flushExpiredBatches() {

        List<SlackMessageBatch> batches =
                batchService.flushExpired(
                        Instant.now()
                );


        for (SlackMessageBatch batch : batches) {

            batchIngestionService.ingest(
                    batch
            );
        }


        if (!batches.isEmpty()) {

            log.info(
                    "Slack batch worker ingested {} expired batch(es).",
                    batches.size()
            );
        }
    }
}