package com.example.demo.analysis;

import java.util.List;

public record ContextAnalysisResult(
        boolean meaningfulChange,
        ChangeType changeType,
        String summary,
        int riskScore,
        double confidence,
        String taskId,
        List<String> impactedNodeIds,
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