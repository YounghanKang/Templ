package com.example.demo.analysis.openai;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OpenAiAnalysisResponse {

    public boolean meaningfulChange;

    public Optional<String> changeType =
            Optional.empty();

    public String summary;

    public int riskScore;

    public double confidence;

    public Optional<String> taskId =
            Optional.empty();

    public List<String> impactedNodeIds =
            List.of();

    public List<String> proposedActions =
            List.of();

    public List<String> openQuestions =
            List.of();


    /*
     * 기존 Adapter 코드를 한 번에 깨뜨리지 않기 위한
     * 호환용 accessor들입니다.
     */

    public boolean meaningfulChange() {
        return meaningfulChange;
    }


    public String changeType() {
        return changeType == null
                ? null
                : changeType.orElse(null);
    }


    public String summary() {
        return summary;
    }


    public int riskScore() {
        return riskScore;
    }


    public double confidence() {
        return confidence;
    }


    public String taskId() {
        return taskId == null
                ? null
                : taskId.orElse(null);
    }


    public List<String> impactedNodeIds() {
        return impactedNodeIds;
    }


    public List<String> proposedActions() {
        return proposedActions;
    }


    public List<String> openQuestions() {
        return openQuestions;
    }
}
