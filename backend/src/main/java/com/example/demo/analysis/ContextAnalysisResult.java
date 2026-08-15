package com.example.demo.analysis;

import java.util.List;
import java.util.UUID;

public record ContextAnalysisResult(
        boolean meaningfulChange,
        ChangeType changeType,
        String summary,
        int riskScore,
        double confidence,
        UUID taskId,
        List<UUID> impactedNodeIds,
        List<String> proposedActions,
        List<String> openQuestions
) {

    public ContextAnalysisResult {

        impactedNodeIds =
                impactedNodeIds == null
                        ? List.of()
                        : List.copyOf(impactedNodeIds);

        proposedActions =
                proposedActions == null
                        ? List.of()
                        : List.copyOf(proposedActions);

        openQuestions =
                openQuestions == null
                        ? List.of()
                        : List.copyOf(openQuestions);
    }
}