package com.example.demo.analysis.openai;

import java.util.List;
import java.util.UUID;

public record OpenAiAnalysisResponse(
        boolean meaningfulChange,
        String changeType,
        String summary,
        int riskScore,
        double confidence,
        UUID primaryNodeId,
        List<UUID> impactedNodeIds,
        List<String> proposedActions,
        List<String> openQuestions
) {

    public OpenAiAnalysisResponse {

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