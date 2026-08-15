package com.example.demo.filter;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "event_filter_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventFilterResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "collaboration_event_id",
            nullable = false
    )
    private UUID collaborationEventId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "decision",
            nullable = false,
            length = 30
    )
    private FilterDecision decision;

    @Column(
            name = "score",
            nullable = false
    )
    private int score;

    /**
     * MVP에서는 List 자체를 별도 테이블로 만들지 않고
     * "|" 문자로 연결한 문자열로 저장
     */
    @Column(
            name = "matched_rules",
            nullable = false,
            length = 1000
    )
    private String matchedRules;

    @Column(
            name = "reason",
            length = 1000
    )
    private String reason;

    @Column(
            name = "filter_version",
            nullable = false,
            length = 30
    )
    private String filterVersion;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;


    public EventFilterResult(
            UUID collaborationEventId,
            FilterDecision decision,
            int score,
            String matchedRules,
            String reason,
            String filterVersion
    ) {
        this.collaborationEventId =
                collaborationEventId;

        this.decision =
                decision;

        this.score =
                score;

        this.matchedRules =
                matchedRules;

        this.reason =
                reason;

        this.filterVersion =
                filterVersion;
    }


    @PrePersist
    protected void onCreate() {

        if (this.createdAt == null) {
            this.createdAt =
                    Instant.now();
        }
    }
}