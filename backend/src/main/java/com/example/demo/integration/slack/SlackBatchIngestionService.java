package com.example.demo.integration.slack;

import com.example.demo.collaboration.CollaborationEventIngestionService;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlackBatchIngestionService {

    private final CollaborationEventIngestionService ingestionService;


    public CollaborationEventIngestionService.IngestionResult ingest(
            SlackMessageBatch batch
    ) {

        if (
                batch == null
                        ||
                        batch.messages() == null
                        ||
                        batch.messages().isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "SlackMessageBatch에는 메시지가 1개 이상 필요합니다."
            );
        }


        List<SlackMessage> messages =
                batch.messages();

        SlackMessage firstMessage =
                messages.get(0);

        SlackMessage lastMessage =
                messages.get(
                        messages.size() - 1
                );


        String externalSourceId =
                buildExternalSourceId(
                        batch,
                        firstMessage,
                        lastMessage
                );

        String title =
                buildTitle(
                        batch
                );

        String content =
                buildContent(
                        messages
                );

        String sourceLocation =
                buildSourceLocation(
                        batch
                );

        Instant occurredAt =
                lastMessage.occurredAt();


        return ingestionService.ingest(
                batch.projectId(),
                SourceTool.SLACK,
                CollaborationEventType.MESSAGE,
                EventAction.CREATED,
                externalSourceId,
                title,
                content,
                null,
                null,
                sourceLocation,
                null,
                false,
                occurredAt
        );
    }


    private String buildExternalSourceId(
            SlackMessageBatch batch,
            SlackMessage firstMessage,
            SlackMessage lastMessage
    ) {

        return "slack-batch:"
                + batch.channelId()
                + ":"
                + messageIdentity(
                firstMessage
        )
                + ":"
                + messageIdentity(
                lastMessage
        );
    }


    private String messageIdentity(
            SlackMessage message
    ) {

        if (
                message.eventId() != null
                        &&
                        !message.eventId().isBlank()
        ) {
            return message.eventId();
        }

        return message.messageTs();
    }


    private String buildTitle(
            SlackMessageBatch batch
    ) {

        if (
                batch.channelName() != null
                        &&
                        !batch.channelName().isBlank()
        ) {
            return "Slack batch: #"
                    + batch.channelName();
        }

        return "Slack batch: "
                + batch.channelId();
    }


    private String buildContent(
            List<SlackMessage> messages
    ) {

        StringBuilder content =
                new StringBuilder();


        content.append(
                "Slack messages:"
        );

        content.append(
                System.lineSeparator()
        );


        for (SlackMessage message : messages) {

            content.append(
                    "- "
            );


            if (message.occurredAt() != null) {

                content.append(
                        "["
                );

                content.append(
                        message.occurredAt()
                );

                content.append(
                        "] "
                );
            }


            if (
                    message.userName() != null
                            &&
                            !message.userName().isBlank()
            ) {

                content.append(
                        message.userName()
                );

            } else if (
                    message.userId() != null
                            &&
                            !message.userId().isBlank()
            ) {

                content.append(
                        message.userId()
                );

            } else {

                content.append(
                        "unknown-user"
                );
            }


            content.append(
                    ": "
            );

            if (message.text() != null) {

                content.append(
                        message.text()
                                .replace(
                                        "\r",
                                        " "
                                )
                                .replace(
                                        "\n",
                                        " "
                                )
                );
            }


            content.append(
                    System.lineSeparator()
            );
        }


        return content.toString()
                .trim();
    }


    private String buildSourceLocation(
            SlackMessageBatch batch
    ) {

        if (
                batch.channelName() != null
                        &&
                        !batch.channelName().isBlank()
        ) {
            return "#"
                    + batch.channelName();
        }

        return batch.channelId();
    }
}