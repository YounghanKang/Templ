package com.example.demo.integration.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.slack.socket-mode.enabled",
        havingValue = "true"
)
public class SlackSocketModeLifecycle {

    private final App slackBoltApp;

    private final String appToken;

    private SocketModeApp socketModeApp;


    public SlackSocketModeLifecycle(
            App slackBoltApp,
            SlackMessageEventListener messageEventListener,
            @Value("${app.slack.app-token:}")
            String appToken
    ) {

        this.slackBoltApp =
                slackBoltApp;

        this.appToken =
                appToken;
    }


    @PostConstruct
    public void start() {

        if (
                appToken == null
                        ||
                        appToken.isBlank()
        ) {
            throw new IllegalStateException(
                    "Slack Socket Mode 사용 시 SLACK_APP_TOKEN이 필요합니다."
            );
        }


        try {

            socketModeApp =
                    new SocketModeApp(
                            appToken,
                            slackBoltApp
                    );


            socketModeApp.startAsync();

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Slack Socket Mode 연결을 시작하지 못했습니다.",
                    exception
            );
        }
    }


    @PreDestroy
    public void stop() {

        if (socketModeApp == null) {
            return;
        }


        try {

            socketModeApp.stop();

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Slack Socket Mode 연결을 종료하지 못했습니다.",
                    exception
            );
        }
    }
}