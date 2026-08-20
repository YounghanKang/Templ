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
        return generateSuggestions(teamId, specId, specText, null, null, null);
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId) {
        return generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId, null);
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId, String existingNodesContext) {
        List<SuggestionDto.Create> out = new ArrayList<>();
        String feedback = userFeedback == null ? "" : userFeedback.trim();
        if (apiKey == null || apiKey.isBlank()) {
            // fallback to stub generator if key is missing
            return new AIStubService().generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId, existingNodesContext);
        }

        try {
            String today = java.time.LocalDate.now().toString();
            String defaultDueDate = java.time.LocalDate.now().plusMonths(1).toString();

            String systemPrompt = """
                    You are a world-class Principal Product Architect and Technical Project Manager.
                    Your mission is to analyze the user's project specification (명세서) and decompose it into a deeply thoughtful, highly realistic, and professional Work Breakdown Structure (WBS).

                    CRITICAL DECOMPOSITION & FEEDBACK INSTRUCTIONS:
                    1. 1:1 EXACT REQUIREMENT EXTRACTION:
                       - Extract all user-specified entities, authentication providers, and features directly into Mid modules and Leaf tasks without omitting any requirements.
                       - Never use generic phase names like "기획", "개발", "테스트". Decompose by real functional domains.

                    2. ZERO REDUNDANCY & STRICT DEDUPLICATION (CRITICAL):
                       - Eliminate any duplicate or redundant nodes. For example, if multiple "단체톡" or duplicate chat/login tasks exist, MERGE them into exactly ONE distinct node.
                       - Each distinct technical responsibility must appear in only ONE module and ONE task.

                    3. USER FEEDBACK COMPLIANCE (HIGHEST PRIORITY):
                       - If the user provides feedback requesting removal, deletion, or exclusion (e.g. "단체톡 없애줘", "중복 제거해줘", "이 기능 빼줘"), you MUST DELETE all corresponding nodes or merge duplicates into one.
                       - NEVER add new duplicate nodes when asked to delete or reduce!
                       - If the user asks to modify or refine a feature, update the relevant node's label and goal directly.
                       - If the user asks to add a new capability, add it to the most relevant module or create a new dedicated module.

                    4. STRUCTURE (Strict 3-Tier Hierarchy):
                       - 1 Root Task (tier="root", tempId="node_root", parentTempId=null): Clear single-sentence ultimate product mission.
                       - 3 to 8 Feature Modules (tier="mid", tempId="node_mid_1", ..., parentTempId="node_root"): Specific functional pillars. ALL mid nodes MUST strictly set parentTempId to "node_root".
                       - Actionable Tasks (tier="leaf", tempId="node_leaf_X", parentTempId matching its parent module's tempId): Concrete implementation units. Generate 0 to 4 leaf nodes per module depending on its complexity. Only break down a module into leaf nodes if it requires detailed technical steps.

                    5. QUALITY STANDARDS & DUE DATE:
                       - dueDate: MUST default to approximately 1 month from today (Today is %s, Target Due Date is %s).
                       - label: Concise, clear naming under 25 characters.
                       - goal: Professional, unambiguous technical deliverable description.
                       - assignees: Realistic roles (e.g. ["Frontend", "Backend", "DevOps", "Designer"]).
                       - aiSummary: Provide sharp, realistic engineering insights, potential friction points, API conflict warnings, or security bottlenecks.
                       - code: Unique identifiers (e.g. Root: T-001, Modules: M-01, M-02, Tasks: T-101, T-102...).

                    6. OUTPUT FORMAT:
                       - Return ONLY a raw JSON array of objects. No markdown backticks, no fences, no commentary.

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
                        "dueDate": "YYYY-MM-DD" (%s),
                        "assignees": ["Role 1", "Role 2"],
                        "aiSummary": "Real-world engineering challenge, security note, or API contract risk"
                      }
                    }
                    """.formatted(today, defaultDueDate, defaultDueDate);

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Project Specification:\n").append(specText).append("\n\n");

            if (existingNodesContext != null && !existingNodesContext.isBlank()) {
                promptBuilder.append("Current WBS Nodes Before Feedback:\n").append(existingNodesContext).append("\n\n");
            }

            if (!feedback.isBlank()) {
                promptBuilder.append("User Feedback / Revision Request (MUST APPLY STRICTLY):\n").append(feedback).append("\n\n");
                promptBuilder.append("Please carefully adjust the WBS according to this feedback. If instructed to remove or deduplicate, delete the redundant tasks. Return the refined 3-tier WBS JSON array targeting due date ").append(defaultDueDate).append(".");
            } else {
                promptBuilder.append("Decompose this project into a deeply customized, feature-driven 3-tier WBS JSON array. Set due dates targeting ").append(defaultDueDate).append(".");
            }

            String userPrompt = promptBuilder.toString();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.2,
                    "max_tokens", 4000
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
                return new AIStubService().generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId, existingNodesContext);
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
