package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "team_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teamId;

    private String userEmail;

    private String username;

    private String nickname;

    @Column(nullable = false)
    private String role; // OWNER, MEMBER

    private Instant joinedAt;
}
