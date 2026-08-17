package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // team id like "T-001"
    private String teamId;

    // e.g., "roadmapNode"
    private String targetType;

    // target identifier (nodeId, etc)
    private String targetId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    // source tool (slack, github, ai)
    private String sourceTool;

    // optional source id (message id, event id)
    private String sourceId;

    // JSON payload describing proposed changes
    @Column(columnDefinition = "TEXT")
    private String changeJson;

    // PENDING / APPROVED / REJECTED
    private String status;

    private String resolvedBy;
    private Instant createdAt;
    private Instant resolvedAt;
}
