package com.example.demo.integration.slack;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.MessageEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SlackMessageEventListenerTest {

    @Test
    @SuppressWarnings("unchecked")
    void messageEventIsConvertedAndProcessed() throws Exception {

        App slackBoltApp =
                mock(
                        App.class
                );

        SlackMessageProcessingService processingService =
                mock(
                        SlackMessageProcessingService.class
                );


        ArgumentCaptor<BoltEventHandler<MessageEvent>> handlerCaptor =
                ArgumentCaptor.forClass(
                        BoltEventHandler.class
                );


        new SlackMessageEventListener(
                slackBoltApp,
                processingService,
                "11111111-1111-1111-1111-111111111111"
        );


        verify(
                slackBoltApp
        ).event(
                eq(MessageEvent.class),
                handlerCaptor.capture()
        );


        MessageEvent event =
                new MessageEvent();

        event.setChannel(
                "C123456"
        );

        event.setUser(
                "U123456"
        );

        event.setText(
                "API 명세를 변경했습니다."
        );

        event.setTs(
                "1690000000.123456"
        );


        EventsApiPayload<MessageEvent> payload =
                mock(
                        EventsApiPayload.class
                );

        when(
                payload.getEventId()
        ).thenReturn(
                "Ev123456"
        );

        when(
                payload.getEvent()
        ).thenReturn(
                event
        );


        EventContext context =
                mock(
                        EventContext.class
                );

        Response response =
                mock(
                        Response.class
                );

        when(
                context.ack()
        ).thenReturn(
                response
        );


        Response actualResponse =
                handlerCaptor
                        .getValue()
                        .apply(
                                payload,
                                context
                        );


        ArgumentCaptor<SlackMessage> messageCaptor =
                ArgumentCaptor.forClass(
                        SlackMessage.class
                );


        verify(
                processingService
        ).process(
                messageCaptor.capture()
        );


        SlackMessage message =
                messageCaptor.getValue();


        assertEquals(
                "11111111-1111-1111-1111-111111111111",
                message.projectId()
        );

        assertEquals(
                "Ev123456",
                message.eventId()
        );

        assertEquals(
                "C123456",
                message.channelId()
        );

        assertNull(
                message.channelName()
        );

        assertEquals(
                "1690000000.123456",
                message.messageTs()
        );

        assertEquals(
                "U123456",
                message.userId()
        );

        assertNull(
                message.userName()
        );

        assertEquals(
                "API 명세를 변경했습니다.",
                message.text()
        );

        assertFalse(
                message.generatedBySystem()
        );

        assertEquals(
                Instant.ofEpochSecond(
                        1690000000L,
                        123456000
                ),
                message.occurredAt()
        );

        assertEquals(
                response,
                actualResponse
        );
    }
    @Test
    @SuppressWarnings("unchecked")
    void botMessageIsMarkedAsGeneratedBySystem() throws Exception {

        App slackBoltApp =
                mock(
                        App.class
                );

        SlackMessageProcessingService processingService =
                mock(
                        SlackMessageProcessingService.class
                );


        ArgumentCaptor<BoltEventHandler<MessageEvent>> handlerCaptor =
                ArgumentCaptor.forClass(
                        BoltEventHandler.class
                );


        new SlackMessageEventListener(
                slackBoltApp,
                processingService,
                "11111111-1111-1111-1111-111111111111"
        );


        verify(
                slackBoltApp
        ).event(
                eq(MessageEvent.class),
                handlerCaptor.capture()
        );


        MessageEvent event =
                new MessageEvent();

        event.setChannel(
                "C123456"
        );

        event.setBotId(
                "B123456"
        );

        event.setText(
                "자동 알림 메시지"
        );

        event.setTs(
                "1690000000.123456"
        );


        EventsApiPayload<MessageEvent> payload =
                mock(
                        EventsApiPayload.class
                );

        when(
                payload.getEventId()
        ).thenReturn(
                "EvBot123456"
        );

        when(
                payload.getEvent()
        ).thenReturn(
                event
        );


        EventContext context =
                mock(
                        EventContext.class
                );

        Response response =
                mock(
                        Response.class
                );

        when(
                context.ack()
        ).thenReturn(
                response
        );


        handlerCaptor
                .getValue()
                .apply(
                        payload,
                        context
                );


        ArgumentCaptor<SlackMessage> messageCaptor =
                ArgumentCaptor.forClass(
                        SlackMessage.class
                );


        verify(
                processingService
        ).process(
                messageCaptor.capture()
        );


        SlackMessage message =
                messageCaptor.getValue();


        assertTrue(
                message.generatedBySystem()
        );
    }
}