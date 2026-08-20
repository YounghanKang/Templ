package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String teamId;

    @Column(nullable = false)
    private String name;

    private Integer members;

    @Column(nullable = false)
    private String color;

    @Column(length = 500)
    private String mission;

    private String ownerUsername;

    private String slackHandle;

    private String githubRepo;
}
