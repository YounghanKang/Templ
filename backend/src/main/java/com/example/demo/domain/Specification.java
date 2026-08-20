package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "specifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String teamId;
    private String author;

    @Column(columnDefinition = "TEXT")
    private String specText;

    private String language;

    // PENDING / ANALYZING / READY
    private String status;

    private Instant createdAt;
}
