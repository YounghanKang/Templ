package com.example.demo.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventAnalysisRepository
        extends JpaRepository<EventAnalysis, UUID> {

    Optional<EventAnalysis>
    findTopByCollaborationEventIdOrderByCreatedAtDesc(
            UUID collaborationEventId
    );


    List<EventAnalysis>
    findByCollaborationEventIdOrderByCreatedAtDesc(
            UUID collaborationEventId
    );


    boolean
    existsByCollaborationEventIdAndRunNumber(
            UUID collaborationEventId,
            int runNumber
    );
}