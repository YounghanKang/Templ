package com.example.demo.analysis;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "event_analysis",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_analysis_event_run",
                        columnNames = {
                                "collaboration_event_id",
                                "run_number"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(
            name = "collaboration_event_id",
            nullable = false
    )
    private UUID collaborationEventId;


    @Column(
            name = "run_number",
            nullable = false
    )
    private int runNumber;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "outcome_type",
            nullable = false,
            length = 30
    )
    private AnalysisOutcomeType outcomeType;


    @Column(
            name = "meaningful_change",
            nullable = false
    )
    private boolean meaningfulChange;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "change_type",
            length = 50
    )
    private ChangeType changeType;


    @Lob
    @Column(
            name = "summary"
    )
    private String summary;


    @Column(
            name = "risk_score",
            nullable = false
    )
    private int riskScore;


    @Column(
            name = "confidence",
            nullable = false
    )
    private double confidence;


    @Column(
            name = "primary_node_id"
    )
    private UUID primaryNodeId;


    /*
     * 실제 OpenAI Adapter를 붙였을 때
     * 어떤 모델/프롬프트/스키마로 분석했는지
     * 추적하기 위한 메타데이터입니다.
     */
    @Column(
            name = "model_name",
            length = 100
    )
    private String modelName;


    @Column(
            name = "prompt_version",
            length = 30
    )
    private String promptVersion;


    @Column(
            name = "schema_version",
            length = 30
    )
    private String schemaVersion;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;


    public EventAnalysis(
            UUID collaborationEventId,
            int runNumber,
            AnalysisOutcomeType outcomeType,
            boolean meaningfulChange,
            ChangeType changeType,
            String summary,
            int riskScore,
            double confidence,
            UUID primaryNodeId,
            String modelName,
            String promptVersion,
            String schemaVersion
    ) {

        if (collaborationEventId == null) {
            throw new IllegalArgumentException(
                    "collaborationEventId는 필수입니다."
            );
        }


        if (runNumber <= 0) {
            throw new IllegalArgumentException(
                    "runNumber는 1 이상이어야 합니다."
            );
        }


        if (outcomeType == null) {
            throw new IllegalArgumentException(
                    "outcomeType은 필수입니다."
            );
        }


        this.collaborationEventId =
                collaborationEventId;

        this.runNumber =
                runNumber;

        this.outcomeType =
                outcomeType;

        this.meaningfulChange =
                meaningfulChange;

        this.changeType =
                changeType;

        this.summary =
                summary;

        this.riskScore =
                riskScore;

        this.confidence =
                confidence;

        this.primaryNodeId =
                primaryNodeId;

        this.modelName =
                modelName;

        this.promptVersion =
                promptVersion;

        this.schemaVersion =
                schemaVersion;

        this.createdAt =
                Instant.now();
    }
}