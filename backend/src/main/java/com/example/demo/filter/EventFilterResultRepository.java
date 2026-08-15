package com.example.demo.filter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventFilterResultRepository
        extends JpaRepository<EventFilterResult, UUID> {

    Optional<EventFilterResult>
    findTopByCollaborationEventIdOrderByCreatedAtDesc(
            UUID collaborationEventId
    );
}