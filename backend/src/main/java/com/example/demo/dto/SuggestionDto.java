package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class SuggestionDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Create {
        private String targetType;
        private String targetId;
        private Long parentSuggestionId;
        private String title;
        private String body;
        private String sourceTool;
        private String sourceId;
        // arbitrary change JSON as string
        private String changeJson;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String teamId;
        private Long parentSuggestionId;
        private String targetType;
        private String targetId;
        private String title;
        private String body;
        private String sourceTool;
        private String sourceId;
        private String changeJson;
        private String status;
        private String resolvedBy;
        private Instant createdAt;
        private Instant resolvedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegenerateRequest {
        private String feedback;
        private Long baseSuggestionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiffResponse {
        private Long baseSuggestionId;
        private Long targetSuggestionId;
        private String baseTitle;
        private String targetTitle;
        private boolean titleChanged;
        private String baseBody;
        private String targetBody;
        private boolean bodyChanged;
        private String baseStatus;
        private String targetStatus;
        private boolean statusChanged;
        private String changeJsonDiff;
    }
}
