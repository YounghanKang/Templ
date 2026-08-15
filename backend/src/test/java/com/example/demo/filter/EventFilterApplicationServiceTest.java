package com.example.demo.filter;

import com.example.demo.collaboration.*;
import com.example.demo.signal.SignalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({
        RuleBasedEventFilter.class,
        EventFilterApplicationService.class
})
class EventFilterApplicationServiceTest {

    @Autowired
    private EventFilterApplicationService
            filterApplicationService;

    @Autowired
    private CollaborationEventRepository
            eventRepository;

    @Autowired
    private EventFilterResultRepository
            filterResultRepository;


    @Test
    void importantMessageBecomesAiPending() {

        CollaborationEvent event =
                new CollaborationEvent(
                        UUID.randomUUID(),
                        SourceTool.SLACK,
                        CollaborationEventType.MESSAGE,
                        EventAction.CREATED,
                        "Ev-filter-001",
                        null,
                        "API 삭제",
                        "U001",
                        "테스트 사용자",
                        "#backend",
                        null,
                        false,
                        Instant.now()
                );

        CollaborationEvent savedEvent =
                eventRepository.save(event);


        FilterResult result =
                filterApplicationService.filter(
                        savedEvent.getId()
                );


        assertEquals(
                FilterDecision.AI_REQUIRED,
                result.decision()
        );


        CollaborationEvent updatedEvent =
                eventRepository
                        .findById(savedEvent.getId())
                        .orElseThrow();

        assertEquals(
                SignalStatus.AI_PENDING,
                updatedEvent.getStatus()
        );


        var storedFilterResult =
                filterResultRepository
                        .findTopByCollaborationEventIdOrderByCreatedAtDesc(
                                savedEvent.getId()
                        );

        assertTrue(
                storedFilterResult.isPresent()
        );

        assertEquals(
                FilterDecision.AI_REQUIRED,
                storedFilterResult
                        .orElseThrow()
                        .getDecision()
        );
    }
}