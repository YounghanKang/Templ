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
        this.maxTokens = Integer.parseInt(System.getProperty("app.ai.max-tokens", System.getenv().getOrDefault("APP_AI_MAX_TOKENS", "3000")));
        this.apiKey = resolveApiKey();
    }

    private String resolveApiKey() {
        // 1순위: 환경변수
        String key = System.getenv("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) {
            logger.info("[OpenAI] API key loaded from environment variable.");
            return key.trim();
        }
        // 2순위: JVM 시스템 프로퍼티
        key = System.getProperty("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) {
            logger.info("[OpenAI] API key loaded from JVM system property.");
            return key.trim();
        }

        // 3순위: .env 파일 직접 파싱 (working directory 기반 + 절대경로 탐색)
        String workDir = System.getProperty("user.dir");
        String[] paths = {
            ".env",
            "backend/.env",
            "../backend/.env",
            "../.env",
            workDir + "/.env",
            workDir + "/backend/.env",
            workDir + "/../backend/.env"
        };
        for (String p : paths) {
            java.io.File f = new java.io.File(p);
            if (f.exists() && f.isFile()) {
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("OPENAI_API_KEY=")) {
                            String v = line.substring("OPENAI_API_KEY=".length()).trim();
                            if (!v.isBlank()) {
                                logger.info("[OpenAI] API key loaded from .env file: {}", f.getAbsolutePath());
                                return v;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("[OpenAI] Failed to read .env at {}: {}", p, e.getMessage());
                }
            }
        }
        logger.warn("[OpenAI] OPENAI_API_KEY not found in environment, JVM properties, or .env files. Working dir: {}", workDir);
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
            logger.warn("[OpenAI] API key is missing — falling back to AIStubService (keyword-based template). " +
                    "Set OPENAI_API_KEY env variable or ensure backend/.env is readable.");
            return new AIStubService().generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId, existingNodesContext);
        }
        logger.info("[OpenAI] Calling OpenAI API (model={}) for team={}, specId={}", model, teamId, specId);

        try {
            String today = java.time.LocalDate.now().toString();
            String defaultDueDate = java.time.LocalDate.now().plusMonths(1).toString();

            String systemPrompt = """
                    You are an expert Project Management Specialist, Agile Coach, and Work Breakdown Structure (WBS) Architect.
                    Your mission is to analyze the user's project specification (명세서) and decompose it into a deeply thoughtful, domain-accurate, highly realistic, and professional Work Breakdown Structure (WBS).

                    CRITICAL GENERATION & DECOMPOSITION RULES:
                    1. DOCUMENT & DOMAIN FIDELITY:
                       - If the user provides a detailed Software Product Specification / PRD (with sections like 핵심 기능, 사용자 플로우, 파이프라인 등):
                         * You MUST decompose the WBS based on the SPECIFIC FEATURES & MODULES explicitly outlined in the PRD!
                         * Map each major functional pillar (e.g., 디렉토리 자동 생성, 할 일 노드 상세화, AI 맥락 분석 및 필터링 파이프라인, 바운더리 침해 감지, 부가 기능) to a dedicated Feature Module (tier="mid").
                         * DO NOT invent generic dummy tasks when concrete features are provided in the spec!
                       - If the project is NON-SOFTWARE / REAL-WORLD (e.g., Purchasing/Procurement, Travel, DIY, Cooking):
                         * NEVER create software/coding/API/database tasks. Decompose into authentic real-world procurement or execution stages.

                    2. ROOT NODE LABEL & TITLE CLEANING:
                       - The root task label MUST NOT contain raw markdown headers, section numbers, or prefixes like "## 1. 서비스 한 줄 정의" or "1. 개요:".
                       - Extract the pure, concise project/service name (e.g., "협업 오케스트레이터 플랫폼", "가상 디렉토리 협업 시스템").

                    3. USER FEEDBACK & REVISION COMPLIANCE (HIGHEST PRIORITY):
                       - If user feedback requests to change node titles/names (e.g. "노드 제목을 '...'로 바꿔줘", "모듈 이름을 변경해줘"):
                         * You MUST IMMEDIATELY CHANGE the root or corresponding module's "label" and "title" to match the requested name!
                       - If user feedback requests to add, split, exclude, or modify tasks:
                         * Strictly apply the changes to the WBS structure, labels, goals, and assignees.

                    4. MANDATORY 3-TIER HIERARCHY (ABSOLUTE REQUIREMENT):
                       - 1 Root Task (tier="root", tempId="node_root", parentTempId=null): Ultimate project vision.
                       - 4 to 6 Feature Modules (tier="mid", tempId="node_mid_1", ..., parentTempId="node_root"): Distinct functional pillars.
                       - Actionable Tasks (tier="leaf", tempId="node_leaf_X", parentTempId matching its parent module's tempId): Concrete deliverables.
                       - CRITICAL: YOU MUST GENERATE EXACTLY 2 TO 4 LEAF NODES (tier="leaf") FOR EVERY SINGLE MID NODE! NEVER return only mid nodes.

                    5. DUE DATE & QUALITY:
                       - dueDate: Target date around %s.
                       - label: Under 25 characters, clean and direct. No root prefix in sub-nodes.
                       - Language: Output all text in Korean if the input is in Korean.

                    6. OUTPUT FORMAT:
                       - Return ONLY a raw JSON array of objects without markdown backticks or commentary.

                    JSON Schema per item:
                    {
                      "title": "Module or Task Title",
                      "body": "Detailed scope and expected output",
                      "changeJson": {
                        "tempId": "node_root" | "node_mid_X" | "node_leaf_X",
                        "parentTempId": null | "node_root" | "node_mid_X",
                        "label": "Short label under 25 chars",
                        "tier": "root" | "mid" | "leaf",
                        "code": "T-001" | "M-01" | "T-101",
                        "goal": "Detailed functional goal",
                        "dueDate": "YYYY-MM-DD" (%s),
                        "assignees": ["Role 1", "Role 2"],
                        "aiSummary": "Domain-specific risk, technical bottleneck, or quality checklist"
                      }
                    }
                    """.formatted(defaultDueDate, defaultDueDate);

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Project Specification:\n").append(specText).append("\n\n");

            if (existingNodesContext != null && !existingNodesContext.isBlank()) {
                promptBuilder.append("Current WBS Nodes Before Feedback:\n").append(existingNodesContext).append("\n\n");
            }

            if (!feedback.isBlank()) {
                promptBuilder.append("User Feedback / Revision Request (MUST APPLY STRICTLY - UPDATE NODE LABELS & RESTRUCTURE):\n").append(feedback).append("\n\n");
                promptBuilder.append("Please carefully reorganize the WBS according to this feedback. If requested to change titles or names, update the node labels and titles directly. Ensure all mid modules have 2~4 leaf tasks. Return the refined 3-tier WBS JSON array targeting due date ").append(defaultDueDate).append(".");
            } else {
                promptBuilder.append("Decompose this project into a domain-accurate, highly professional 3-tier WBS JSON array (1 root, 4~6 mid modules, 2~4 leaf tasks per module). Return only the JSON array.");
            }

            String userPrompt = promptBuilder.toString();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", temperature,
                    "max_tokens", 4000
            );

            String reqJson = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(50))
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
                    List<SuggestionDto.Create> outList = new ArrayList<>();
                    List<Map<String, Object>> midItems = new ArrayList<>();
                    boolean hasLeaf = false;

                    for (int idx = 0; idx < items.size(); idx++) {
                        Map<String, Object> item = items.get(idx);
                        Object titleObj = item.get("title");
                        Object bodyObj = item.get("body");
                        String title = titleObj != null ? titleObj.toString().trim() : "";
                        String bodyText = bodyObj != null ? bodyObj.toString().trim() : "";

                        // Handle both nested changeJson and flat structure
                        Map<String, Object> changeMap = new java.util.LinkedHashMap<>();
                        Object change = item.get("changeJson");
                        if (change instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) change;
                            changeMap.putAll(m);
                        } else if (change instanceof String) {
                            try {
                                JsonNode p = objectMapper.readTree((String) change);
                                changeMap = objectMapper.convertValue(p, new TypeReference<Map<String, Object>>() {});
                            } catch (Exception ignored) {}
                        }

                        // Check flat fields if changeJson missing
                        if (!changeMap.containsKey("tempId") && item.containsKey("tempId")) changeMap.put("tempId", item.get("tempId"));
                        if (!changeMap.containsKey("parentTempId") && item.containsKey("parentTempId")) changeMap.put("parentTempId", item.get("parentTempId"));
                        if (!changeMap.containsKey("tier") && item.containsKey("tier")) changeMap.put("tier", item.get("tier"));
                        if (!changeMap.containsKey("label") && item.containsKey("label")) changeMap.put("label", item.get("label"));
                        if (!changeMap.containsKey("code") && item.containsKey("code")) changeMap.put("code", item.get("code"));
                        if (!changeMap.containsKey("goal") && item.containsKey("goal")) changeMap.put("goal", item.get("goal"));
                        if (!changeMap.containsKey("assignees") && item.containsKey("assignees")) changeMap.put("assignees", item.get("assignees"));
                        if (!changeMap.containsKey("aiSummary") && item.containsKey("aiSummary")) changeMap.put("aiSummary", item.get("aiSummary"));

                        // Defaults & cleaning
                        String tier = changeMap.containsKey("tier") && changeMap.get("tier") != null ? changeMap.get("tier").toString() : (idx == 0 ? "root" : "leaf");
                        if (idx == 0 && !"root".equalsIgnoreCase(tier)) tier = "root";
                        changeMap.put("tier", tier);

                        if (!changeMap.containsKey("tempId") || changeMap.get("tempId") == null) {
                            changeMap.put("tempId", "root".equalsIgnoreCase(tier) ? "node_root" : ("mid".equalsIgnoreCase(tier) ? "node_mid_" + idx : "node_leaf_" + idx));
                        }
                        if ("root".equalsIgnoreCase(tier)) {
                            changeMap.put("parentTempId", null);
                        }

                        String label = changeMap.containsKey("label") && changeMap.get("label") != null ? changeMap.get("label").toString() : (title.isBlank() ? "작업 " + (idx + 1) : title);
                        // Clean markdown artifacts from label
                        label = label.replaceAll("^[#*`>\\s]+", "").replaceAll("^\\d+\\.\\s*", "").trim();
                        changeMap.put("label", label);

                        if (title.isBlank()) title = label;
                        if (bodyText.isBlank()) bodyText = changeMap.containsKey("goal") && changeMap.get("goal") != null ? changeMap.get("goal").toString() : label;

                        if (!changeMap.containsKey("dueDate") || changeMap.get("dueDate") == null || changeMap.get("dueDate").toString().isBlank()) {
                            changeMap.put("dueDate", defaultDueDate);
                        }

                        if ("leaf".equalsIgnoreCase(tier)) {
                            hasLeaf = true;
                        } else if ("mid".equalsIgnoreCase(tier)) {
                            midItems.add(changeMap);
                        }

                        String changeStr = objectMapper.writeValueAsString(changeMap);
                        SuggestionDto.Create s = SuggestionDto.Create.builder()
                                .targetType("roadmapNode")
                                .targetId(null)
                                .title(title)
                                .body(bodyText)
                                .sourceTool("openai")
                                .sourceId(String.valueOf(specId))
                                .changeJson(changeStr)
                                .build();
                        outList.add(s);
                    }

                    // If LLM returned only mid nodes and no leaf nodes, synthesize leaf nodes automatically to prevent fallback
                    if (!hasLeaf && !midItems.isEmpty()) {
                        logger.info("OpenAI returned {} mid nodes without leaves. Synthesizing leaf nodes to maintain 3-tier WBS.", midItems.size());
                        int leafGlobalIdx = 10;
                        for (Map<String, Object> mid : midItems) {
                            String midTempId = (String) mid.get("tempId");
                            String midLabel = (String) mid.get("label");
                            String midGoal = mid.get("goal") != null ? mid.get("goal").toString() : midLabel;

                            // Create 2 leaf tasks per mid module
                            String l1Title = midLabel + " 설계 및 세부 요구사항 정의";
                            String l1Goal = midGoal + " - 아키텍처 및 인터페이스 설계";
                            Map<String, Object> leaf1Map = Map.of(
                                    "tempId", midTempId + "_task_1",
                                    "parentTempId", midTempId,
                                    "label", truncateOneLine(l1Title, 25),
                                    "tier", "leaf",
                                    "code", String.format("T-%03d", leafGlobalIdx++),
                                    "goal", l1Goal,
                                    "dueDate", defaultDueDate,
                                    "assignees", mid.get("assignees") != null ? mid.get("assignees") : List.of("담당자"),
                                    "aiSummary", "세부 설계 및 사전 검토 필수"
                            );
                            outList.add(SuggestionDto.Create.builder()
                                    .targetType("roadmapNode")
                                    .targetId(null)
                                    .title(l1Title)
                                    .body(l1Goal)
                                    .sourceTool("openai")
                                    .sourceId(String.valueOf(specId))
                                    .changeJson(objectMapper.writeValueAsString(leaf1Map))
                                    .build());

                            String l2Title = midLabel + " 기능 구현 및 검증";
                            String l2Goal = midGoal + " - 핵심 로직 구현 및 단위 테스트";
                            Map<String, Object> leaf2Map = Map.of(
                                    "tempId", midTempId + "_task_2",
                                    "parentTempId", midTempId,
                                    "label", truncateOneLine(l2Title, 25),
                                    "tier", "leaf",
                                    "code", String.format("T-%03d", leafGlobalIdx++),
                                    "goal", l2Goal,
                                    "dueDate", defaultDueDate,
                                    "assignees", mid.get("assignees") != null ? mid.get("assignees") : List.of("담당자"),
                                    "aiSummary", "핵심 플로우 및 예외 처리 검증"
                            );
                            outList.add(SuggestionDto.Create.builder()
                                    .targetType("roadmapNode")
                                    .targetId(null)
                                    .title(l2Title)
                                    .body(l2Goal)
                                    .sourceTool("openai")
                                    .sourceId(String.valueOf(specId))
                                    .changeJson(objectMapper.writeValueAsString(leaf2Map))
                                    .build());
                        }
                        hasLeaf = true;
                    }

                    if (outList.size() >= 3 && hasLeaf) {
                        return outList;
                    }
                } catch (Exception e) {
                    logger.warn("Failed parsing OpenAI JSON output: {}", e.getMessage());
                }
            }

        } catch (Exception ex) {
            logger.error("Exception during OpenAI call: {}", ex.getMessage(), ex);
        }

        // Guardrail: If OpenAI fails or returns < 3 tasks, use stub generator
        return new AIStubService().generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId, existingNodesContext);
    }

    @Override
    public List<SuggestionDto.Create> translateSuggestions(List<SuggestionDto.Create> suggestions, String targetLang) {
        if (targetLang == null || targetLang.isBlank() || suggestions.isEmpty()) return suggestions;
        if (targetLang.toLowerCase().startsWith("ko")) return suggestions; // Already generated in Korean
        
        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String inputJson = objectMapper.writeValueAsString(suggestions);
                String prompt = "Translate the 'title', 'body', and 'aiSummary' (inside 'changeJson' object) fields of the following JSON array of objects into the language code: " + targetLang + ".\n"
                        + "CRITICAL: Do NOT change any other fields (like 'tempId', 'parentTempId', 'tier', 'code', 'dueDate', 'assignees'). Keep the exact same JSON array structure.\n"
                        + "Return ONLY a raw JSON array. No markdown, no fences.\n\n"
                        + inputJson;

                Map<String, Object> reqBody = Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", "You are an expert technical translator. You must return only a valid JSON array."),
                                Map.of("role", "user", "content", prompt)
                        ),
                        "temperature", 0.1,
                        "max_tokens", 4000
                );

                String reqJson = objectMapper.writeValueAsString(reqBody);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() / 100 != 2) {
                    logger.error("OpenAI API returned non-2xx code {}, body: {} during translation", resp.statusCode(), resp.body());
                    if (attempt == maxRetries) return suggestions;
                    continue;
                }

                JsonNode root = objectMapper.readTree(resp.body());
                String response = root.path("choices").path(0).path("message").path("content").asText("");
                String jsonOutput = normalizeJsonResponse(response);
                
                List<Map<String, Object>> items = objectMapper.readValue(jsonOutput, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                List<SuggestionDto.Create> out = new java.util.ArrayList<>();
                for (Map<String, Object> item : items) {
                    SuggestionDto.Create s = SuggestionDto.Create.builder()
                            .targetType((String) item.get("targetType"))
                            .targetId((String) item.get("targetId"))
                            .title((String) item.get("title"))
                            .body((String) item.get("body"))
                            .sourceTool((String) item.get("sourceTool"))
                            .sourceId((String) item.get("sourceId"))
                            .changeJson(item.get("changeJson") instanceof String ? (String) item.get("changeJson") : objectMapper.writeValueAsString(item.get("changeJson")))
                            .build();
                    out.add(s);
                }
                if (out.size() == suggestions.size()) {
                    return out;
                } else {
                    logger.warn("Translated output size {} differs from input size {}", out.size(), suggestions.size());
                }
            } catch (Exception e) {
                logger.error("Failed to translate suggestions (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
            }
        }
        return suggestions;
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
