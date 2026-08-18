package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class EventFilterDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FilterRequest {
        private String sourceTool; // slack, github, etc.
        private String content;
        private String previousContent; // optional for edit distance check
        private Boolean generatedBySystem;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FilterResultResponse {
        private boolean passed; // whether it passed filter and requires AI
        private String decision; // PASS_TO_AI, FILTERED_OUT, LOG_ONLY
        private double impactScore;
        private int editDistance;
        private List<String> matchedKeywords;
        private String reason;
    }
}
