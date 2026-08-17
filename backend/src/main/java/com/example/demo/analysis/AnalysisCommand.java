package com.example.demo.analysis;

import java.util.List;
import java.util.UUID;

public record AnalysisCommand(
        String projectId,
        UUID eventId,
        String sourceTool,
        String sourceLocation,
        String eventTitle,
        String eventContent,
        String actorName,
        List<AnalysisCandidate> candidates
) {

    public AnalysisCommand {

        candidates =
                candidates == null
                        ? List.of()
                        : List.copyOf(candidates);
    }
}