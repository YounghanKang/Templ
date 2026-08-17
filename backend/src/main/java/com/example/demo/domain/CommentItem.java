package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teamId;

    @Column(nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private String time;
}
