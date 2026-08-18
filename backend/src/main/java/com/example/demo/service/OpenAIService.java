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
        this.apiKey = resolveApiKey();
    }

    private String resolveApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) return key.trim();
        key = System.getProperty("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) return key.trim();

        // Fallback: check .env files in working directory or parent
        String[] paths = { ".env", "backend/.env", "../backend/.env", "../.env" };
        for (String p : paths) {
            java.io.File f = new java.io.File(p);
            if (f.exists() && f.isFile()) {
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("OPENAI_API_KEY=")) {
                            String v = line.substring("OPENAI_API_KEY=".length()).trim();
                            if (!v.isBlank()) return v;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
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
            String today = java.time.LocalDate.now().toString();
            String defaultDueDate = java.time.LocalDate.now().plusMonths(1).toString();

            String systemPrompt = """
                    You are a world-class Principal Product Architect and Technical Project Manager.
                    Your mission is to analyze the user's project idea/specification (명세서) and decompose it into a deeply thoughtful, highly realistic, and professional Work Breakdown Structure (WBS).

                    CRITICAL DECOMPOSITION PRINCIPLES:
                    1. 1:1 EXACT REQUIREMENT EXTRACTION & DECOMPOSITION:
                       - If the user specifies particular features, platforms, or tools (e.g. "로그인은 카카오 네이버로", "동아리 사이트", "회원가입"), you MUST extract them directly into dedicated Mid modules and Leaf tasks without dropping any details.
                       - Example for "로그인 회원가입이 되는 동아리 사이트를 만들어줘, 로그인은 카카오 네이버로":
                         * Root: 동아리 웹 사이트 구축
                         * Mid 1: 소셜 로그인 시스템 -> Leaf: 카카오 로그인 연동, Leaf: 네이버 로그인 연동
                         * Mid 2: 회원가입 및 프로필 관리 -> Leaf: 동아리 회원가입 폼 및 약관 동의, Leaf: 부원 프로필 및 권한 관리
                         * Mid 3: 동아리 활동 및 커뮤니티 -> Leaf: 공지사항 및 게시판, Leaf: 활동 일정 캘린더 및 갤러리
                       - Never drop user-specified technologies, auth providers, or feature constraints.

                    2. DO NOT use generic phase names like "기획", "개발", "테스트". 
                       Decompose by real functional domains (Pillars).

                    3. STRUCTURE (Strict 3-Tier Hierarchy):
                       - 1 Root Task (tier="root", tempId="node_root", parentTempId=null): Clear single-sentence ultimate product mission.
                       - 3 to 4 Feature Modules (tier="mid", tempId="node_mid_1", ..., parentTempId="node_root"): Specific functional pillars directly matching the user's requirements.
                       - 2 to 3 Actionable Tasks per Module (tier="leaf", tempId="node_leaf_X", parentTempId matching its parent module): Concrete implementation units (e.g. REST API, UI view, DB schema, SDK integration).

                    4. QUALITY STANDARDS & DUE DATE (CRITICAL):
                       - dueDate: MUST default to approximately 1 month from today (Today is %s, Target Due Date is around %s).
                       - label: Concise, clear naming (e.g. "카카오 로그인 연동", "네이버 로그인 연동", "부원 권한 관리").
                       - goal: Professional, unambiguous technical deliverable description.
                       - assignees: Realistic roles (e.g. ["Frontend", "Backend", "Product Designer", "DevOps"]).
                       - aiSummary: Provide sharp, realistic engineering insights, potential friction points, API conflict warnings, or security bottlenecks.
                       - code: Unique identifiers (e.g. Root: T-001, Modules: M-01, M-02, Tasks: T-101, T-102...).

                    5. USER FEEDBACK HANDLING:
                       - If user feedback is provided, prioritize modifying, adding, or replacing modules/tasks to explicitly fulfill the user's instructions.

                    6. OUTPUT FORMAT:
                       - Return ONLY a raw JSON array of objects. No markdown backticks, no fences, no explanation.

                    JSON Schema per item:
                    {
                      "title": "Module or Task Title",
                      "body": "Detailed technical scope and expected output",
                      "changeJson": {
                        "tempId": "node_root" | "node_mid_X" | "node_leaf_X",
                        "parentTempId": null | "node_root" | "node_mid_X",
                        "label": "Short label under 25 chars",
                        "tier": "root" | "mid" | "leaf",
                        "code": "T-001" | "M-01" | "T-101",
                        "goal": "Detailed functional goal",
                        "dueDate": "YYYY-MM-DD" (around %s),
                        "assignees": ["Role 1", "Role 2"],
                        "aiSummary": "Real-world engineering challenge, security note, or API contract risk"
                      }
                    }
                    """.formatted(today, defaultDueDate, defaultDueDate);

            String userPrompt = "Project Specification:\n" + specText + "\n\n" +
                    (feedback.isBlank() ? "" : "User Feedback to Incorporate with High Priority:\n" + feedback + "\n\n") +
                    "Decompose this project into a deeply customized, feature-driven 3-tier WBS JSON array. Make sure every specific requirement and entity in the specification is represented in the modules and tasks. Set due dates targeting " + defaultDueDate + ".";

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.3,
                    "max_tokens", 2500
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
                logger.error("OpenAI API returned non-2xx code {}, body: {}, falling back to stub", resp.statusCode(), resp.body());
                return new AIStubService().generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId);
            }

            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
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
                                changeStr = objectMapper.writeValueAsString(Map.of("aiSummary", bodyText, "dueDate", defaultDueDate));
                            } else if (change instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> map = (Map<String, Object>) change;
                                if (!map.containsKey("dueDate") || map.get("dueDate") == null || map.get("dueDate").toString().isBlank()) {
                                    map.put("dueDate", defaultDueDate);
                                }
                                changeStr = objectMapper.writeValueAsString(map);
                            } else if (change instanceof String) {
                                JsonNode parsed = objectMapper.readTree((String) change);
                                changeStr = objectMapper.writeValueAsString(parsed);
                            } else {
                                changeStr = objectMapper.writeValueAsString(change.toString());
                            }
                        } catch (Exception ce) {
                            changeStr = "{\"aiSummary\": \"" + escapeJson(truncateOneLine(bodyText, 200)) + "\", \"dueDate\": \"" + defaultDueDate + "\"}";
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
