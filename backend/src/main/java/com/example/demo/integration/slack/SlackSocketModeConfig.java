package com.example.demo.integration.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "app.slack.socket-mode.enabled",
        havingValue = "true"
)
public class SlackSocketModeConfig {

    @Bean
    public App slackBoltApp(
            @Value("${app.slack.bot-token:}")
            String botToken
    ) {

        if (
                botToken == null
                        ||
                        botToken.isBlank()
        ) {
            throw new IllegalStateException(
                    "Slack Socket Mode 사용 시 SLACK_BOT_TOKEN이 필요합니다."
            );
        }


        AppConfig appConfig =
                AppConfig.builder()
                        .singleTeamBotToken(
                                botToken
                        )
                        .build();


        return new App(
                appConfig
        );
    }
}