package com.example.demo.integration.slack;

import com.example.demo.filter.FilterResult;
import com.example.demo.filter.RuleBasedEventFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.filter.FilterDecision;
import java.util.Optional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SlackMessageBatchService {

    private static final int MAX_MESSAGES_PER_BATCH = 5;

    private static final Duration MAX_BATCH_AGE =
            Duration.ofMinutes(5);

    private final RuleBasedEventFilter ruleBasedEventFilter;

    private final Map<BatchKey, List<BufferedMessage>> buffers =
            new ConcurrentHashMap<>();


    private record BatchKey(
            UUID projectId,
            String channelId
    ) {
    }


    private record BufferedMessage(
            SlackMessage message,
            FilterResult filterResult,
            Instant bufferedAt
    ) {
    }
    public synchronized Optional<SlackMessageBatch> accept(
            SlackMessage message
    ) {

        return accept(
                message,
                Instant.now()
        );
    }


    synchronized Optional<SlackMessageBatch> accept(
            SlackMessage message,
            Instant bufferedAt
    ) {

        FilterResult filterResult =
                ruleBasedEventFilter.evaluate(
                        message.text(),
                        message.generatedBySystem()
                );


        BatchKey key =
                new BatchKey(
                        message.projectId(),
                        message.channelId()
                );


        List<BufferedMessage> buffer =
                buffers.computeIfAbsent(
                        key,
                        ignored ->
                                new ArrayList<>()
                );


        buffer.add(
                new BufferedMessage(
                        message,
                        filterResult,
                        bufferedAt
                )
        );


        if (
                buffer.size()
                        < MAX_MESSAGES_PER_BATCH
        ) {
            return Optional.empty();
        }


        buffers.remove(
                key
        );


        return createBatchIfMeaningful(
                buffer
        );
    }
    public synchronized List<SlackMessageBatch> flushExpired(
            Instant now
    ) {

        List<SlackMessageBatch> batches =
                new ArrayList<>();


        List<BatchKey> expiredKeys =
                buffers.entrySet()
                        .stream()
                        .filter(
                                entry -> {

                                    List<BufferedMessage> buffer =
                                            entry.getValue();


                                    if (buffer.isEmpty()) {
                                        return false;
                                    }


                                    Instant firstBufferedAt =
                                            buffer.get(0)
                                                    .bufferedAt();


                                    if (firstBufferedAt == null) {
                                        return false;
                                    }

                                    return !now.isBefore(
                                            firstBufferedAt.plus(
                                                    MAX_BATCH_AGE
                                            )
                                    );
                                }
                        )
                        .map(
                                Map.Entry::getKey
                        )
                        .toList();


        for (BatchKey key : expiredKeys) {

            List<BufferedMessage> buffer =
                    buffers.remove(
                            key
                    );


            if (
                    buffer == null
                            ||
                            buffer.isEmpty()
            ) {
                continue;
            }


            createBatchIfMeaningful(
                    buffer
            ).ifPresent(
                    batches::add
            );
        }


        return List.copyOf(
                batches
        );
    }
    private Optional<SlackMessageBatch> createBatchIfMeaningful(
            List<BufferedMessage> buffer
    ) {

        boolean containsAiRequired =
                buffer.stream()
                        .anyMatch(
                                buffered ->
                                        buffered
                                                .filterResult()
                                                .decision()
                                                == FilterDecision.AI_REQUIRED
                        );


        if (!containsAiRequired) {
            return Optional.empty();
        }


        List<SlackMessage> messages =
                buffer.stream()
                        .map(
                                BufferedMessage::message
                        )
                        .toList();


        SlackMessage firstMessage =
                messages.get(0);


        return Optional.of(
                new SlackMessageBatch(
                        firstMessage.projectId(),
                        firstMessage.channelId(),
                        firstMessage.channelName(),
                        messages
                )
        );
    }
    int bufferedMessageCount(
            UUID projectId,
            String channelId
    ) {

        List<BufferedMessage> buffer =
                buffers.get(
                        new BatchKey(
                                projectId,
                                channelId
                        )
                );

        return buffer == null
                ? 0
                : buffer.size();
    }
}