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
        this.maxTokens = Integer.parseInt(System.getProperty("app.ai.max-tokens", System.getenv().getOrDefault("APP_AI_MAX_TOKENS", "1500")));
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
            // fallback to stub generator if key is missing
            return new AIStubService().generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId);
        }

        try {
            String systemPrompt = """
                    You are an expert AI collaboration orchestrator and technical project manager specializing in Work Breakdown Structures (WBS) and Virtual Directory Hierarchies.
                    Analyze the project specification (명세서) and break it down into a comprehensive, highly actionable 3-tier hierarchical WBS (루트 최종 목표 -> 주요 모듈/기능 -> 세부 실행 태스크).
                    
                    CRITICAL REQUIREMENTS:
                    1. Return ONLY a valid, raw JSON array of objects. No markdown backticks, no markdown fences, no explanatory text.
                    2. Maintain language consistency: if the spec is in Korean, generate all titles, labels, goals, and summaries in natural, professional Korean.
                    3. Structure across 3 tiers:
                       - 1 Root task (tier="root", tempId="node_root", parentTempId=null): The overarching project goal.
                       - 2 to 4 Mid-level modules (tier="mid", tempId="node_mid_1", "node_mid_2", ..., parentTempId="node_root"): Core feature domains/milestones.
                       - 2 to 3 Leaf tasks per Mid module (tier="leaf", tempId="node_leaf_1", ..., parentTempId matching its parent Mid module): Concrete implementation tasks.
                    4. Ensure total tasks are between 6 and 12 items.
                    
                    Each array item must strictly follow this JSON schema:
                    {
                      "title": "Task or Module Title",
                      "body": "Detailed description of scope and expected deliverables",
                      "changeJson": {
                        "tempId": "node_root" | "node_mid_X" | "node_leaf_X",
                        "parentTempId": null | "node_root" | "node_mid_X",
                        "label": "Concise display label (under 25 chars)",
                        "tier": "root" | "mid" | "leaf",
                        "code": "T-101" (unique code formatted T-XXX),
                        "goal": "Clear, measurable goal for this specific task",
                        "dueDate": "YYYY-MM-DD" (reasonable milestone dates e.g. within 1-3 months),
                        "assignees": ["Role/Name", ...], (e.g. ["Frontend", "Backend", "Product", "Designer"]),
                        "aiSummary": "AI insight or risk warning regarding potential friction, API conflicts, or key validation points"
                      }
                    }
                    """;

            String userPrompt = "Project Specification:\n" + specText + "\n\n" +
                    (feedback.isBlank() ? "" : "User Feedback to Incorporate:\n" + feedback + "\n\n") +
                    "Generate the complete 3-tier WBS JSON array with accurate tempId and parentTempId tree references.";

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
                logger.warn("OpenAI API returned non-2xx code {}, falling back to stub", resp.statusCode());
                return new AIStubService().generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId);
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
                        Object titleObj = item.get("title");
                        Object bodyObj = item.get("body");
                        if (titleObj == null || bodyObj == null) continue;

                        String title = titleObj.toString().trim();
                        String bodyText = bodyObj.toString().trim();
                        if (title.isBlank() || bodyText.isBlank()) continue;

                        Object change = item.get("changeJson");
                        String changeStr;
                        try {
                            if (change == null) {
                                changeStr = objectMapper.writeValueAsString(Map.of("aiSummary", bodyText));
                            } else if (change instanceof Map) {
                                changeStr = objectMapper.writeValueAsString(change);
                            } else if (change instanceof String) {
                                JsonNode parsed = objectMapper.readTree((String) change);
                                changeStr = objectMapper.writeValueAsString(parsed);
                            } else {
                                changeStr = objectMapper.writeValueAsString(change.toString());
                            }
                        } catch (Exception ce) {
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
                    if (out.size() >= 3) return out;
                } catch (Exception e) {
                    logger.warn("Failed parsing OpenAI JSON output: {}", e.getMessage());
                }
            }

        } catch (Exception ex) {
            logger.error("Exception during OpenAI call: {}", ex.getMessage(), ex);
        }

        // Guardrail: If OpenAI fails or returns < 3 tasks, use stub generator
        return new AIStubService().generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId);
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
