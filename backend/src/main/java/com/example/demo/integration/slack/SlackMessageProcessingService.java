package com.example.demo.integration.slack;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlackMessageProcessingService {

    private final SlackMessageBatchService batchService;

    private final SlackBatchIngestionService batchIngestionService;


    public void process(
            SlackMessage message
    ) {

        batchService.accept(
                message
        ).ifPresent(
                batchIngestionService::ingest
        );
    }
}