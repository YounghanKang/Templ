package com.example.demo.filter;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import com.example.demo.signal.SignalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventFilterApplicationService {

    private final CollaborationEventRepository eventRepository;

    private final EventFilterResultRepository
            filterResultRepository;

    private final RuleBasedEventFilter
            ruleBasedEventFilter;


    @Transactional
    public FilterResult filter(
            UUID collaborationEventId
    ) {

        CollaborationEvent event =
                eventRepository
                        .findById(collaborationEventId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "CollaborationEvent not found: "
                                                        + collaborationEventId
                                        )
                        );


        /*
         * 1. 규칙 기반 필터 실행
         */
        FilterResult result =
                ruleBasedEventFilter.evaluate(event);


        /*
         * 2. Filter 결과 DB 저장
         */
        String matchedRules =
                String.join(
                        "|",
                        result.matchedRules()
                );

        EventFilterResult entity =
                new EventFilterResult(
                        event.getId(),
                        result.decision(),
                        result.score(),
                        matchedRules,
                        result.reason(),
                        result.filterVersion()
                );

        filterResultRepository.save(entity);


        /*
         * 3. CollaborationEvent 상태 변경
         */
        switch (result.decision()) {

            case FILTERED_OUT ->
                    event.changeStatus(
                            SignalStatus.FILTERED_OUT
                    );

            case LOG_ONLY ->
                    event.changeStatus(
                            SignalStatus.LOG_ONLY
                    );

            case AI_REQUIRED ->
                    event.changeStatus(
                            SignalStatus.AI_PENDING
                    );
        }


        return result;
    }
}