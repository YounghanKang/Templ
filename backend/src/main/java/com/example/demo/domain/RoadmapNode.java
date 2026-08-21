package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private String teamId;

    private String label;
    private String code;
    private String status;
    private Integer progress;
    private String goal;
    private String dueDate;
    private String aiSummary;
    private String issue;
    private String tier;
    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;

    @Column(columnDefinition = "TEXT")
    private String assigneesJson;

    @Column(columnDefinition = "TEXT")
    private String prerequisitesJson;

    @Column(columnDefinition = "TEXT")
    private String filesJson;

    @Column(columnDefinition = "TEXT")
    private String commentsJson;
}
