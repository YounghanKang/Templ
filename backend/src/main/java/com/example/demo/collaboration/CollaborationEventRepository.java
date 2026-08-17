package com.example.demo.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollaborationEventRepository
        extends JpaRepository<CollaborationEvent, UUID> {

    List<CollaborationEvent> findByProjectIdOrderByCreatedAtDesc(
            String projectId
    );

    boolean existsByProjectIdAndSourceToolAndExternalSourceId(
            String projectId,
            SourceTool sourceTool,
            String externalSourceId
    );
}