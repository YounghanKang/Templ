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

                    CRITICAL DOMAIN FIDELITY RULES:
                    1. DOMAIN CLASSIFICATION & STRICT ADAPTATION:
                       - FIRST, detect the true nature and domain of the project:
                         a) NON-SOFTWARE / REAL-WORLD TASKS (e.g. Purchasing & Procurement like "컴퓨터 부품(SSD, RAM, CPU) 구매", "스피커 구매하기", Equipment/Supplies Purchase, Event/Trip Planning, Marketing Campaign, Office Relocation, Crafting/DIY, Cooking, Study/Certification, Research, Daily Life Goals).
                         b) SOFTWARE / IT TASKS (e.g. Web App Development, Mobile App, Backend API, Database Design, Cloud Infrastructure, Coding).
                       - IF THE PROJECT IS NON-SOFTWARE / REAL-WORLD:
                         * ABSOLUTELY FORBIDDEN: NEVER invent software development, website creation, app development, coding, database, backend/frontend, API endpoints, UI screens, server deployment, or DevOps tasks!
                         * You MUST decompose the project into authentic, domain-specific execution stages (e.g. for purchasing: 1. 요구 사양 및 호환성/예산 정의, 2. 부품/후보 모델 스펙 및 벤치마크 비교, 3. 최적 판매처 선정 및 주문 결제, 4. 배송 수령, 장착/조립 및 정상 동작 검수).
                         * ASSIGNEES: Must reflect real-world roles (e.g., ["구매 담당", "하드웨어/장비 담당", "예산 관리자", "검수 담당", "기획자"]). NEVER use ["Frontend", "Backend", "DevOps"] for non-software projects!
                         * aiSummary: Must describe realistic real-world considerations, procurement risks, compatibility issues, warranty terms, or quality bottlenecks (e.g., "CPU 소켓/메인보드 칩셋 호환성 및 RAM DDR 규격 일치 여부 사전 확인 필수, 초기 불량 A/S 정책 검토").

                    2. NO ROOT PREFIX DUPLICATION IN SUB-NODES (CRITICAL UI REQUIREMENT):
                       - The root task already contains the full project name.
                       - Mid and Leaf node labels and titles MUST NEVER prepend or repeat the root project title!
                       - WRONG: "컴퓨터 부품(SSD, RAM, CPU) 구매 - 요구 사양 및 예산 설정" (REDUNDANT AND UGLY)
                       - RIGHT: "요구 사양 및 부품 간 호환성 정의" (CLEAN, DIRECT, PROFESSIONAL)
                       - WRONG: "스피커 구매하기 - 후보 모델 스펙 비교"
                       - RIGHT: "후보 모델 스펙 및 음질 비교 분석"

                    3. USER FEEDBACK COMPLIANCE & STRICT EXCLUSION/REMOVAL (HIGHEST PRIORITY):
                       - If user feedback requests to remove, exclude, or delete a specific task, topic, or feature (e.g. "실구매자 리뷰 빼줘", "리뷰는 제외해줘", "A 기능 빼줘"):
                         * You MUST COMPLETELY EXCLUDE AND DELETE that task or topic from all nodes and descriptions!
                         * Replace with technical specification, benchmark comparison, or warranty checks instead.
                       - If user asks to add a new requirement (e.g. "스피커 선도 구매하고 싶어", "서멀구리스도 추가"):
                         * NEVER use raw conversational user phrases (e.g. NEVER title a module "스피커 선도 구매하고 싶어").
                         * Extract the core requirement (e.g. "호환 케이블 및 소모품 구비") and REORGANIZE/RESTRUCTURE the whole WBS naturally.

                    4. STRUCTURE (Strict 3-Tier Hierarchy):
                       - 1 Root Task (tier="root", tempId="node_root", parentTempId=null): Clear single-sentence ultimate project mission.
                       - 3 to 6 Feature Modules (tier="mid", tempId="node_mid_1", ..., parentTempId="node_root"): Distinct, non-overlapping pillars. ALL mid nodes MUST strictly set parentTempId to "node_root".
                       - Actionable Tasks (tier="leaf", tempId="node_leaf_X", parentTempId matching its parent module's tempId): Concrete, practical execution units. YOU MUST GENERATE EXACTLY 2 TO 4 LEAF NODES FOR EVERY SINGLE MID NODE. DO NOT SKIP ANY LEAF NODES.

                    5. QUALITY STANDARDS & DUE DATE:
                       - dueDate: MUST default to approximately 1 month from today (Today is %s, Target Due Date is %s).
                       - label: Concise, clear naming under 25 characters without root prefix.
                       - goal: Professional, unambiguous deliverable description.
                       - code: Unique identifiers (e.g. Root: T-001, Modules: M-01, M-02, Tasks: T-101, T-102...).

                    6. LANGUAGE:
                       - Output ALL titles, labels, goals, bodies, and aiSummaries in Korean if the input is in Korean.

                    7. OUTPUT FORMAT:
                       - Return ONLY a raw JSON array of objects. No markdown backticks, no fences, no commentary.

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
                        "aiSummary": "Domain-specific risk, technical bottleneck, compatibility notice, or quality checklist"
                      }
                    }
                    """.formatted(today, defaultDueDate, defaultDueDate);

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Project Specification:\n").append(specText).append("\n\n");

            if (existingNodesContext != null && !existingNodesContext.isBlank()) {
                promptBuilder.append("Current WBS Nodes Before Feedback:\n").append(existingNodesContext).append("\n\n");
            }

            if (!feedback.isBlank()) {
                promptBuilder.append("User Feedback / Revision Request (MUST APPLY STRICTLY & RESTRUCTURE WBS):\n").append(feedback).append("\n\n");
                promptBuilder.append("Please carefully reorganize and restructure the WBS according to this feedback. If asked to remove/exclude a task (like reviews), strictly remove it. Do not prefix mid/leaf titles with root title. Return the refined 3-tier WBS JSON array targeting due date ").append(defaultDueDate).append(".");
            } else {
                promptBuilder.append("Decompose this project into a domain-accurate, highly professional 3-tier WBS JSON array. Do not duplicate root prefix in sub-nodes. If non-software, do not create software tasks. Set due dates targeting ").append(defaultDueDate).append(".");
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
                    .timeout(Duration.ofSeconds(45))
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
                    boolean hasLeaf = false;
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
                        outList.add(s);
                        if (s.getChangeJson() != null && (s.getChangeJson().contains("\"tier\":\"leaf\"") || s.getChangeJson().contains("\"tier\": \"leaf\""))) {
                            hasLeaf = true;
                        }
                    }
                    if (outList.size() >= 3 && hasLeaf) {
                        return outList;
                    } else if (!hasLeaf) {
                        logger.warn("OpenAI returned {} nodes but ZERO leaf nodes. Falling back to stub.", outList.size());
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
