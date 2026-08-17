package com.example.demo.service;

import com.example.demo.dto.SuggestionDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub", matchIfMissing = true)
public class AIStubService implements AiService {

    /**
     * Very small heuristic stub that converts a spec text into a few suggestion DTOs.
     * Real LLM integration should replace this.
     */
    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText) {
        return generateSuggestions(teamId, specId, specText, null, null);
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId) {
        List<SuggestionDto.Create> out = new ArrayList<>();
        String trimmedFeedback = userFeedback == null ? "" : userFeedback.trim();
        boolean hasFeedback = !trimmedFeedback.isBlank();

        String titleRoot = hasFeedback ? "AI 재생성: 사용자 의견 반영" : "AI 제안: 로드맵 뼈대 생성";
        String bodyRoot = hasFeedback
                ? "사용자 피드백을 반영해 재구성한 로드맵 초안입니다.\n사용자 의견: " + trimmedFeedback
                : "명세를 바탕으로 생성된 초기 로드맵 뼈대입니다. 승인하면 로드맵에 반영됩니다.";

        String changeJsonRoot = String.format("{\"label\": \"%s\", \"aiSummary\": \"%s\", \"feedback\": \"%s\"}",
                escapeJson(truncateOneLine(specText, 40)), escapeJson(truncateOneLine(specText, 120) + (hasFeedback ? " / 사용자 피드백: " + trimmedFeedback : "")), escapeJson(trimmedFeedback));

        SuggestionDto.Create root = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title(titleRoot)
                .body(bodyRoot)
                .sourceTool("ai-stub")
                .sourceId(String.valueOf(specId))
                .changeJson(changeJsonRoot)
                .build();
        out.add(root);

        String[] parts = specText == null ? new String[0] : specText.split("[\\.\n]");
        int limit = Math.min(parts.length, 3);
        for (int i = 0; i < limit; i++) {
            String p = parts[i].trim();
            if (p.isBlank()) continue;
            String body = hasFeedback ? "사용자 의견 반영: " + trimmedFeedback + "\n세부 작업 제안: " + truncateOneLine(p, 200) : "세부 작업 제안: " + truncateOneLine(p, 200);
            SuggestionDto.Create s = SuggestionDto.Create.builder()
                    .targetType("roadmapNode")
                    .targetId(null)
                    .title(truncateOneLine(p, 50))
                    .body(body)
                    .sourceTool("ai-stub")
                    .sourceId(String.valueOf(specId))
                    .changeJson(String.format("{\"label\": \"%s\", \"aiSummary\": \"%s\"}", escapeJson(truncateOneLine(p, 40)), escapeJson(body)))
                    .build();
            out.add(s);
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
