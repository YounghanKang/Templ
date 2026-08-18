package com.example.demo.collaboration;

import com.example.demo.signal.SignalStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "collaboration_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollaborationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Backend1의 Project를 직접 Entity 관계로 묶지 않고
     * UUID만 저장
     *
     * Backend2는 ProjectNode/Project를 직접 수정 X
     */
    @Column(name = "project_id", nullable = false)
    private String projectId;

    /**
     * 이벤트 발생 도구
     * INTERNAL_CHAT / SLACK / GITHUB
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_tool", nullable = false, length = 30)
    private SourceTool sourceTool;

    /**
     * MESSAGE / ISSUE / PUSH / PULL_REQUEST
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private CollaborationEventType eventType;

    /**
     * CREATED / UPDATED / DELETED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_action", nullable = false, length = 20)
    private EventAction eventAction;

    /**
     * Slack event_id, GitHub delivery id 등
     * 외부 시스템의 원본 식별자
     */
    @Column(name = "external_source_id", length = 200)
    private String externalSourceId;

    @Column(name = "title", length = 500)
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "source_location", length = 500)
    private String sourceLocation;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    /**
     * 우리 서비스 또는 시스템이 만든 이벤트인지 여부.
     * true면 AI 분석 전에 제거
     */
    @Column(name = "generated_by_system", nullable = false)
    private boolean generatedBySystem;

    /**
     * 현재 Backend2 처리 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private SignalStatus status;

    /**
     * 실제 협업 이벤트 발생 시각
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    public CollaborationEvent(
            String projectId,
            SourceTool sourceTool,
            CollaborationEventType eventType,
            EventAction eventAction,
            String externalSourceId,
            String title,
            String content,
            String actorId,
            String actorName,
            String sourceLocation,
            String sourceUrl,
            boolean generatedBySystem,
            Instant occurredAt
    ) {
        this.projectId = projectId;
        this.sourceTool = sourceTool;
        this.eventType = eventType;
        this.eventAction = eventAction;
        this.externalSourceId = externalSourceId;
        this.title = title;
        this.content = content;
        this.actorId = actorId;
        this.actorName = actorName;
        this.sourceLocation = sourceLocation;
        this.sourceUrl = sourceUrl;
        this.generatedBySystem = generatedBySystem;
        this.occurredAt = occurredAt;

        this.status = SignalStatus.RECEIVED;
    }


    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (this.occurredAt == null) {
            this.occurredAt = now;
        }

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = SignalStatus.RECEIVED;
        }
    }


    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }


    public void changeStatus(
            SignalStatus status
    ) {
        this.status = status;
    }
}