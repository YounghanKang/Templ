package com.example.demo.service;

import com.example.demo.dto.SuggestionDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAIService implements AiService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final String apiKey;

    public OpenAIService() {
        this.model = System.getProperty("app.ai.model", System.getenv().getOrDefault("APP_AI_MODEL", "gpt-4o-mini"));
        this.temperature = Double.parseDouble(System.getProperty("app.ai.temperature", System.getenv().getOrDefault("APP_AI_TEMPERATURE", "0.2")));
        this.maxTokens = Integer.parseInt(System.getProperty("app.ai.max-tokens", System.getenv().getOrDefault("APP_AI_MAX_TOKENS", "800")));
        this.apiKey = System.getenv("OPENAI_API_KEY");
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText) {
        return generateSuggestions(teamId, specId, specText, null, null);
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId) {
        List<SuggestionDto.Create> out = new ArrayList<>();
        String feedback = userFeedback == null ? "" : userFeedback.trim();
        if (apiKey == null || apiKey.isBlank()) {
            String fallbackBody = feedback.isBlank()
                    ? "OPENAI_API_KEY가 설정되어 있지 않습니다. env var OPENAI_API_KEY를 설정하세요."
                    : "OPENAI_API_KEY가 설정되어 있지 않지만, 사용자 피드백을 반영해 로드맵 재생성 후보를 준비했습니다.\n사용자 의견: " + feedback;
            SuggestionDto.Create fallback = SuggestionDto.Create.builder()
                    .targetType("roadmapNode")
                    .targetId(null)
                    .title(feedback.isBlank() ? "AI 제안(오픈AI 사용 불가) - 요약" : "AI 재생성(오픈AI 사용 불가) - 사용자 의견 반영")
                    .body(fallbackBody)
                    .sourceTool("openai-fallback")
                    .sourceId(String.valueOf(specId))
                    .changeJson(String.format("{\"label\": \"%s\", \"aiSummary\": \"%s\", \"feedback\": \"%s\"}", escapeJson(truncateOneLine(specText, 40)), escapeJson(truncateOneLine(specText, 200) + (feedback.isBlank() ? "" : " / 사용자 피드백: " + feedback)), escapeJson(feedback)))
                    .build();
            out.add(fallback);
            return out;
        }

        try {
            String systemPrompt = "You are a helpful assistant that converts a project specification into a list of suggestion cards. " +
                    "Return ONLY raw JSON, with no markdown fences, no code fences, and no commentary. " +
                    "Respond with a strict JSON array. Each array item must be an object with keys: title (string), body (string), changeJson (JSON object with fields like label and aiSummary). " +
                    "If the user provides feedback, treat it as an instruction that must be reflected in the regenerated suggestions. " +
                    "Keep the original spec as the main source of truth, but revise the roadmap suggestions to satisfy the user's feedback.";
            String userPrompt = "Spec:\n" + specText + "\n\n" +
                    (feedback.isBlank() ? "" : "User feedback:\n" + feedback + "\n\n") +
                    "Produce up to 5 suggestion objects as described. Return only raw JSON." +
                    (baseSuggestionId == null ? "" : "\nThe current suggestion being revised has id=" + baseSuggestionId + ".");

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", temperature,
                    "max_tokens", maxTokens
            );

            String reqJson = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                // non-2xx
                SuggestionDto.Create err = SuggestionDto.Create.builder()
                        .targetType("roadmapNode")
                        .targetId(null)
                        .title("AI 제안 오류")
                        .body("OpenAI 호출 실패: status=" + resp.statusCode())
                        .sourceTool("openai")
                        .sourceId(String.valueOf(specId))
                        .changeJson(String.format("{\"label\": \"%s\", \"aiSummary\": \"%s\"}", escapeJson(truncateOneLine(specText, 40)), escapeJson(truncateOneLine(specText, 200))))
                        .build();
                out.add(err);
                return out;
            }

            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message").path("content");
                String content = message.asText();
                String normalized = normalizeJsonResponse(content);
                try {
                    List<Map<String, Object>> items = objectMapper.readValue(normalized, new TypeReference<>() {});
                    for (Map<String, Object> item : items) {
                        // validation: require title and body
                        Object titleObj = item.get("title");
                        Object bodyObj = item.get("body");
                        if (titleObj == null || bodyObj == null) {
                            // skip invalid item
                            continue;
                        }
                        String title = titleObj.toString().trim();
                        String bodyText = bodyObj.toString().trim();
                        if (title.isBlank() || bodyText.isBlank()) {
                            continue;
                        }

                        // normalize changeJson field
                        Object change = item.get("changeJson");
                        String changeStr;
                        try {
                            if (change == null) {
                                changeStr = objectMapper.writeValueAsString(Map.of("aiSummary", bodyText));
                            } else if (change instanceof Map) {
                                changeStr = objectMapper.writeValueAsString(change);
                            } else if (change instanceof String) {
                                String changeCandidate = (String) change;
                                // try parsing string as JSON; if fails, wrap into aiSummary
                                try {
                                    JsonNode parsed = objectMapper.readTree(changeCandidate);
                                    changeStr = objectMapper.writeValueAsString(parsed);
                                } catch (Exception pe) {
                                    changeStr = objectMapper.writeValueAsString(Map.of("aiSummary", changeCandidate));
                                }
                            } else {
                                // unknown type, stringify
                                changeStr = objectMapper.writeValueAsString(change.toString());
                            }
                        } catch (Exception ce) {
                            // if any serialization issue, fallback
                            changeStr = "{\"aiSummary\": \"" + escapeJson(truncateOneLine(bodyText, 200)) + "\"}";
                        }

                        SuggestionDto.Create s = SuggestionDto.Create.builder()
                                .targetType("roadmapNode")
                                .targetId(null)
                                .title(title)
                                .body(bodyText)
                                .sourceTool("openai")
                                .sourceId(String.valueOf(specId))
                                .changeJson(changeStr)
                                .build();
                        out.add(s);
                    }
                    if (!out.isEmpty()) return out;
                } catch (Exception e) {
                    // parsing failed; fall through to fallback
                }
                // fallback: wrap content into one suggestion
                SuggestionDto.Create single = SuggestionDto.Create.builder()
                        .targetType("roadmapNode")
                        .targetId(null)
                        .title(truncateOneLine(content, 80))
                        .body(truncateOneLine(content, 600))
                        .sourceTool("openai")
                        .sourceId(String.valueOf(specId))
                        .changeJson(String.format("{\"aiSummary\": \"%s\"}", escapeJson(truncateOneLine(content, 400))))
                        .build();
                out.add(single);
            }

        } catch (Exception ex) {
            SuggestionDto.Create err = SuggestionDto.Create.builder()
                    .targetType("roadmapNode")
                    .targetId(null)
                    .title("AI 처리 실패")
                    .body("OpenAI 처리 중 예외: " + ex.getMessage())
                    .sourceTool("openai")
                    .sourceId(String.valueOf(specId))
                    .changeJson(String.format("{\"label\": \"%s\", \"aiSummary\": \"%s\"}", escapeJson(truncateOneLine(specText, 40)), escapeJson(truncateOneLine(specText, 200))))
                    .build();
            out.add(err);
        }

        return out;
    }

    private String normalizeJsonResponse(String content) {
        if (content == null) return "[]";
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start >= 0 && end > start) {
                trimmed = trimmed.substring(start + 1, end).trim();
            } else {
                trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
            }
        }
        return trimmed;
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
