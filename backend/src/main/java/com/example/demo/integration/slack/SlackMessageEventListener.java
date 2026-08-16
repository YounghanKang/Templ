package com.example.demo.integration.slack;

import com.slack.api.bolt.App;
import com.slack.api.model.event.MessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "app.slack.socket-mode.enabled",
        havingValue = "true"
)
public class SlackMessageEventListener {

    private final SlackMessageProcessingService processingService;

    private final UUID projectId;


    public SlackMessageEventListener(
            App slackBoltApp,
            SlackMessageProcessingService processingService,
            @Value("${app.slack.project-id:}")
            String projectId
    ) {

        this.processingService =
                processingService;

        this.projectId =
                parseProjectId(
                        projectId
                );


        slackBoltApp.event(
                MessageEvent.class,
                (payload, context) -> {

                    MessageEvent event =
                            payload.getEvent();


                    SlackMessage message =
                            new SlackMessage(
                                    this.projectId,
                                    payload.getEventId(),
                                    event.getChannel(),
                                    null,
                                    event.getTs(),
                                    event.getUser(),
                                    null,
                                    event.getText(),
                                    isGeneratedBySystem(
                                            event
                                    ),
                                    parseSlackTimestamp(
                                            event.getTs()
                                    )
                            );


                    this.processingService.process(
                            message
                    );


                    return context.ack();
                }
        );
    }


    private UUID parseProjectId(
            String projectId
    ) {

        if (
                projectId == null
                        ||
                        projectId.isBlank()
        ) {
            throw new IllegalStateException(
                    "Slack Socket Mode 사용 시 SLACK_PROJECT_ID가 필요합니다."
            );
        }


        try {

            return UUID.fromString(
                    projectId
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "SLACK_PROJECT_ID는 올바른 UUID 형식이어야 합니다.",
                    exception
            );
        }
    }


    private boolean isGeneratedBySystem(
            MessageEvent event
    ) {

        return (
                event.getBotId() != null
                        &&
                        !event.getBotId().isBlank()
        )
                ||
                event.getBotProfile() != null;
    }


    private Instant parseSlackTimestamp(
            String timestamp
    ) {

        if (
                timestamp == null
                        ||
                        timestamp.isBlank()
        ) {
            return null;
        }


        try {

            String[] parts =
                    timestamp.split(
                            "\\.",
                            2
                    );

            long seconds =
                    Long.parseLong(
                            parts[0]
                    );

            int nanos = 0;


            if (parts.length == 2) {

                String fraction =
                        parts[1];


                String nanosText =
                        (
                                fraction
                                        + "000000000"
                        ).substring(
                                0,
                                9
                        );


                nanos =
                        Integer.parseInt(
                                nanosText
                        );
            }


            return Instant.ofEpochSecond(
                    seconds,
                    nanos
            );

        } catch (RuntimeException exception) {

            return null;
        }
    }
}