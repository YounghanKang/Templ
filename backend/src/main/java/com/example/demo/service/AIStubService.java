package com.example.demo.service;

import com.example.demo.dto.SuggestionDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub", matchIfMissing = true)
public class AIStubService implements AiService {

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText) {
        return generateSuggestions(teamId, specId, specText, null, null);
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId) {
        List<SuggestionDto.Create> out = new ArrayList<>();
        String trimmedFeedback = userFeedback == null ? "" : userFeedback.trim();
        boolean hasFeedback = !trimmedFeedback.isBlank();

        String rawText = (specText == null || specText.isBlank()) ? "프로젝트 기본 기능 개발 명세서" : specText;
        String[] parts = rawText.split("[\\.\n,]+");
        List<String> validParts = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isBlank()) validParts.add(t);
        }

        // 1. Root task (tempId: node_root)
        String rootLabel = truncateOneLine(validParts.isEmpty() ? "핵심 프로젝트 목표" : validParts.get(0), 40);
        String rootTitle = hasFeedback ? "AI 재생성: " + rootLabel : "AI 제안: " + rootLabel;
        String rootBody = hasFeedback
                ? "사용자 피드백을 반영한 프로젝트 최상위 로드맵 목표입니다.\n피드백: " + trimmedFeedback
                : "명세서 분석 결과 도출된 프로젝트 최상위 목표입니다.";

        String rootJson = String.format(
                "{\"tempId\": \"node_root\", \"parentTempId\": null, \"label\": \"%s\", \"tier\": \"root\", \"goal\": \"%s\", \"aiSummary\": \"%s\"}",
                escapeJson(rootLabel), escapeJson(rootTitle), escapeJson(rootBody)
        );

        out.add(SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title(rootTitle)
                .body(rootBody)
                .sourceTool("ai-stub")
                .sourceId(String.valueOf(specId))
                .changeJson(rootJson)
                .build());

        // 2. Mid modules & Leaf tasks (tempId / parentTempId linked)
        int midCount = Math.max(2, Math.min(validParts.size(), 3));
        int globalCounter = 1;

        for (int m = 1; m <= midCount; m++) {
            String midTempId = "node_mid_" + m;
            String midLabel = (m - 1 < validParts.size())
                    ? truncateOneLine(validParts.get(m - 1), 35)
                    : "모듈 " + m + " 구현 및 설계";
            String midTitle = "중분류: " + midLabel;
            String midBody = "프로젝트 주요 실행 모듈 " + m + "입니다. " + (hasFeedback ? "피드백 반영: " + trimmedFeedback : "");

            String midJson = String.format(
                    "{\"tempId\": \"%s\", \"parentTempId\": \"node_root\", \"label\": \"%s\", \"tier\": \"mid\", \"goal\": \"%s\", \"aiSummary\": \"%s\"}",
                    midTempId, escapeJson(midLabel), escapeJson(midTitle), escapeJson(midBody)
            );

            out.add(SuggestionDto.Create.builder()
                    .targetType("roadmapNode")
                    .targetId(null)
                    .title(midTitle)
                    .body(midBody)
                    .sourceTool("ai-stub")
                    .sourceId(String.valueOf(specId))
                    .changeJson(midJson)
                    .build());

            // 2 Leaf tasks per Mid module
            for (int l = 1; l <= 2; l++) {
                String leafTempId = "node_leaf_" + globalCounter++;
                String leafLabel = midLabel + " - 세부 작업 " + l;
                String leafTitle = "하위 작업: " + leafLabel;
                String leafBody = "세부 기능 구현 및 단위 테스트 (" + leafLabel + ")";

                String leafJson = String.format(
                        "{\"tempId\": \"%s\", \"parentTempId\": \"%s\", \"label\": \"%s\", \"tier\": \"leaf\", \"goal\": \"%s\", \"aiSummary\": \"%s\"}",
                        leafTempId, midTempId, escapeJson(leafLabel), escapeJson(leafTitle), escapeJson(leafBody)
                );

                out.add(SuggestionDto.Create.builder()
                        .targetType("roadmapNode")
                        .targetId(null)
                        .title(leafTitle)
                        .body(leafBody)
                        .sourceTool("ai-stub")
                        .sourceId(String.valueOf(specId))
                        .changeJson(leafJson)
                        .build());
            }
        }

        return out;
    }

    private String truncateOneLine(String s, int max) {
        if (s == null) return "";
        String one = s.replaceAll("\\s+", " ").trim();
        if (one.length() <= max) return one;
        return one.substring(0, max - 3) + "...";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
