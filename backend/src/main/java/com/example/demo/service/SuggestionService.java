package com.example.demo.service;

import com.example.demo.config.SuggestionPolicyProperties;
import com.example.demo.domain.Specification;
import com.example.demo.domain.Suggestion;
import com.example.demo.dto.RoadmapDto;
import com.example.demo.dto.SuggestionDto;
import com.example.demo.repository.SpecificationRepository;
import com.example.demo.repository.SuggestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final SpecificationRepository specificationRepository;
    private final RoadmapService roadmapService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiService aiService;
    private final SuggestionPolicyProperties policyProperties;

    public SuggestionService(SuggestionRepository suggestionRepository, SpecificationRepository specificationRepository, RoadmapService roadmapService, AiService aiService, SuggestionPolicyProperties policyProperties) {
        this.suggestionRepository = suggestionRepository;
        this.specificationRepository = specificationRepository;
        this.roadmapService = roadmapService;
        this.aiService = aiService;
        this.policyProperties = policyProperties;
    }

    private RoadmapDto.RoadmapNodeDto toNodeDtoFromJson(JsonNode item) {
        RoadmapDto.RoadmapNodeDto dto = new RoadmapDto.RoadmapNodeDto();
        if (item.has("label")) dto.setLabel(item.get("label").asText());
        else if (item.has("title")) dto.setLabel(item.get("title").asText());
        if (item.has("code")) dto.setCode(item.get("code").asText());
        if (item.has("aiSummary")) dto.setAiSummary(item.get("aiSummary").asText());
        else if (item.has("body")) dto.setAiSummary(item.get("body").asText());
        if (item.has("goal")) dto.setGoal(item.get("goal").asText());
        if (item.has("dueDate")) dto.setDueDate(item.get("dueDate").asText());
        if (item.has("tier")) dto.setTier(item.get("tier").asText());
        if (item.has("status")) dto.setStatus(item.get("status").asText());
        if (item.has("progress")) dto.setProgress(item.get("progress").asInt());
        if (item.has("x")) dto.setX(item.get("x").asInt());
        if (item.has("y")) dto.setY(item.get("y").asInt());
        if (item.has("w")) dto.setW(item.get("w").asInt());
        if (item.has("h")) dto.setH(item.get("h").asInt());
        if (item.has("assignees") && item.get("assignees").isArray()) {
            java.util.List<String> asg = new java.util.ArrayList<>();
            item.get("assignees").forEach(n -> asg.add(n.asText()));
            dto.setAssignees(asg);
        }
        if (dto.getStatus() == null) dto.setStatus("todo");
        if (dto.getProgress() == null) dto.setProgress(0);
        if (dto.getAssignees() == null) dto.setAssignees(new java.util.ArrayList<>());
        if (dto.getGoal() == null) dto.setGoal("");
        if (dto.getDueDate() == null) dto.setDueDate("2026-09-30");
        return dto;
    }

    public SuggestionDto.Response create(String teamId, SuggestionDto.Create request) {
        Suggestion s = Suggestion.builder()
                .teamId(teamId)
                .parentSuggestionId(request.getParentSuggestionId())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .title(request.getTitle())
                .body(request.getBody())
                .sourceTool(request.getSourceTool())
                .sourceId(request.getSourceId())
                .changeJson(request.getChangeJson())
                .status("PENDING")
                .createdAt(Instant.now())
                .build();
        Suggestion saved = suggestionRepository.save(s);
        return toDto(saved);
    }

    public List<SuggestionDto.Response> listForTeam(String teamId) {
        return suggestionRepository.findAllByTeamIdOrderByIdDesc(teamId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<SuggestionDto.Response> get(String teamId, Long id) {
        return suggestionRepository.findById(id).filter(s -> teamId.equals(s.getTeamId())).map(this::toDto);
    }

    @Transactional
    public List<SuggestionDto.Response> regenerate(String teamId, Long suggestionId, SuggestionDto.RegenerateRequest request) {
        Suggestion base = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("suggestion not found: " + suggestionId));
        if (!teamId.equals(base.getTeamId())) throw new IllegalArgumentException("team mismatch");

        // Check regeneration depth limit from policy
        int currentDepth = getHistory(teamId, suggestionId).size();
        int maxDepth = policyProperties != null ? policyProperties.getMaxRegenerationDepth() : 5;
        if (currentDepth >= maxDepth) {
            throw new IllegalStateException("Exceeded max regeneration depth limit of " + maxDepth);
        }

        String feedback = request == null || request.getFeedback() == null ? "" : request.getFeedback().trim();
        Long specId = null;
        if (base.getSourceId() != null && !base.getSourceId().isBlank()) {
            try {
                specId = Long.parseLong(base.getSourceId());
            } catch (NumberFormatException ignored) {
                // sourceId may not be numeric for other external AI events; fall back to base suggestion id
            }
        }
        final Long effectiveSpecId = specId == null ? suggestionId : specId;

        Specification spec = specificationRepository.findById(effectiveSpecId)
                .orElseThrow(() -> new IllegalArgumentException("spec not found for suggestion regeneration: " + effectiveSpecId));

        List<SuggestionDto.Create> generated = aiService.generateSuggestions(teamId, effectiveSpecId, spec.getSpecText(), feedback, suggestionId);
        List<SuggestionDto.Response> result = new ArrayList<>();
        for (SuggestionDto.Create candidate : generated) {
            candidate.setParentSuggestionId(suggestionId);
            if (candidate.getSourceId() == null || candidate.getSourceId().isBlank()) {
                candidate.setSourceId(String.valueOf(specId));
            }
            if (candidate.getSourceTool() == null || candidate.getSourceTool().isBlank()) {
                candidate.setSourceTool("ai-regenerate");
            }
            if (candidate.getBody() == null) {
                candidate.setBody("");
            }
            if (!feedback.isBlank() && !candidate.getBody().contains(feedback)) {
                candidate.setBody(candidate.getBody() + "\n\n사용자 의견: " + feedback);
            }
            if (candidate.getTargetType() == null || candidate.getTargetType().isBlank()) {
                candidate.setTargetType("roadmapNode");
            }
            result.add(create(teamId, candidate));
        }
        return result;
    }

    @Transactional
    public SuggestionDto.Response approve(String teamId, Long id, String approver) {
        Suggestion s = suggestionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("suggestion not found: " + id));
        if (!teamId.equals(s.getTeamId())) throw new IllegalArgumentException("team mismatch");
        if (!"PENDING".equals(s.getStatus())) throw new IllegalStateException("suggestion not pending");

        if ("roadmapNode".equalsIgnoreCase(s.getTargetType()) && s.getChangeJson() != null && !s.getChangeJson().isBlank()) {
            try {
                if (s.getSourceId() != null && suggestionRepository.existsBySourceIdAndStatus(s.getSourceId(), "APPROVED")) {
                    // already applied by another suggestion from same source
                } else {
                    var rootNode = objectMapper.readTree(s.getChangeJson());
                    if (rootNode.isArray() || rootNode.has("nodes") || rootNode.has("items")) {
                        java.util.List<JsonNode> items = new java.util.ArrayList<>();
                        if (rootNode.isArray()) {
                            rootNode.forEach(items::add);
                        } else if (rootNode.has("nodes") && rootNode.get("nodes").isArray()) {
                            rootNode.get("nodes").forEach(items::add);
                        } else if (rootNode.has("items") && rootNode.get("items").isArray()) {
                            rootNode.get("items").forEach(items::add);
                        }

                        if (!items.isEmpty()) {
                            String rootId = null;
                            RoadmapDto.RoadmapNodeDto rootDto = toNodeDtoFromJson(items.get(0));
                            if (s.getTargetId() != null) {
                                roadmapService.updateNode(teamId, s.getTargetId(), rootDto);
                                rootId = s.getTargetId();
                            } else {
                                var createdRoot = roadmapService.createNode(teamId, rootDto);
                                rootId = createdRoot.getId();
                            }

                            for (int i = 1; i < items.size(); i++) {
                                var childJson = items.get(i);
                                RoadmapDto.RoadmapNodeDto childDto = toNodeDtoFromJson(childJson);
                                var createdChild = roadmapService.createNode(teamId, childDto);
                                RoadmapDto.RoadmapEdgeDto edge = RoadmapDto.RoadmapEdgeDto.builder()
                                        .id(null).from(rootId).to(createdChild.getId()).build();
                                roadmapService.createEdge(teamId, edge);
                            }
                        }
                    } else if (s.getTargetId() == null && s.getSourceId() != null) {
                        var allForSource = suggestionRepository.findAllByTeamIdOrderByIdDesc(teamId).stream()
                                .filter(x -> s.getSourceId().equals(x.getSourceId()) && "PENDING".equals(x.getStatus()))
                                .collect(Collectors.toList());
                        if (!allForSource.isEmpty()) {
                            allForSource.sort(java.util.Comparator.comparing(Suggestion::getId));

                            // 1. Parse metadata for all suggestions
                            class NodeMeta {
                                Suggestion s;
                                JsonNode json;
                                String tempId;
                                String parentTempId;
                                String tier;
                                RoadmapDto.RoadmapNodeDto dto;
                                NodeMeta(Suggestion s, JsonNode json, String tempId, String parentTempId, String tier, RoadmapDto.RoadmapNodeDto dto) {
                                    this.s = s; this.json = json; this.tempId = tempId; this.parentTempId = parentTempId; this.tier = tier; this.dto = dto;
                                }
                            }

                            List<NodeMeta> metas = new java.util.ArrayList<>();
                            for (int i = 0; i < allForSource.size(); i++) {
                                Suggestion currS = allForSource.get(i);
                                JsonNode json = objectMapper.readTree(currS.getChangeJson());
                                RoadmapDto.RoadmapNodeDto dto = toNodeDtoFromJson(json);
                                String tempId = json.has("tempId") ? json.get("tempId").asText() : "node_" + (i + 1);
                                String parentTempId = json.has("parentTempId") && !json.get("parentTempId").isNull() ? json.get("parentTempId").asText() : null;
                                String tier = (i == 0) ? "root" : (dto.getTier() != null ? dto.getTier() : "leaf");
                                if (i == 0) parentTempId = null;
                                metas.add(new NodeMeta(currS, json, tempId, parentTempId, tier, dto));
                            }

                            // 2. Classify hierarchy
                            NodeMeta rootMeta = metas.stream().filter(m -> "root".equalsIgnoreCase(m.tier) || m.parentTempId == null).findFirst().orElse(metas.get(0));
                            List<NodeMeta> midMetas = metas.stream().filter(m -> m != rootMeta && ("mid".equalsIgnoreCase(m.tier) || rootMeta.tempId.equals(m.parentTempId))).toList();
                            if (midMetas.isEmpty() && metas.size() > 1) {
                                midMetas = metas.subList(1, Math.min(metas.size(), 4));
                            }
                            final List<NodeMeta> finalMidMetas = midMetas;
                            List<NodeMeta> leafMetas = metas.stream().filter(m -> m != rootMeta && !finalMidMetas.contains(m)).toList();

                            // 3. Compute coordinates
                            int LEAF_W = 128, LEAF_H = 58, MID_W = 182, MID_H = 66, ROOT_W = 240, ROOT_H = 74;
                            int PAD_X = 46, GROUP_GAP = 46, LEAF_GAP = 16;
                            int ROOT_Y = 22, MID_Y = 216, LEAF_Y = 396;

                            int cursor = PAD_X;
                            Map<String, String> tempIdToRealId = new java.util.HashMap<>();

                            // Create mid nodes & their leaf children
                            for (int m = 0; m < midMetas.size(); m++) {
                                NodeMeta mid = midMetas.get(m);
                                List<NodeMeta> children = leafMetas.stream()
                                        .filter(l -> mid.tempId.equals(l.parentTempId))
                                        .toList();
                                if (children.isEmpty() && !leafMetas.isEmpty()) {
                                    int perMid = Math.max(1, leafMetas.size() / midMetas.size());
                                    int start = m * perMid;
                                    int end = (m == midMetas.size() - 1) ? leafMetas.size() : Math.min(leafMetas.size(), (m + 1) * perMid);
                                    if (start < leafMetas.size()) {
                                        children = leafMetas.subList(start, end);
                                    }
                                }

                                int groupW = children.isEmpty() ? MID_W : children.size() * LEAF_W + (children.size() - 1) * LEAF_GAP;
                                int actualGroupW = Math.max(groupW, MID_W);

                                // Mid node coordinates
                                int mx = cursor + (actualGroupW / 2) - (MID_W / 2);
                                mid.dto.setTier("mid");
                                mid.dto.setX(mx);
                                mid.dto.setY(MID_Y);
                                mid.dto.setW(MID_W);
                                mid.dto.setH(MID_H);

                                var createdMid = roadmapService.createNode(teamId, mid.dto);
                                tempIdToRealId.put(mid.tempId, createdMid.getId());

                                // Leaf children coordinates & create
                                for (int l = 0; l < children.size(); l++) {
                                    NodeMeta leaf = children.get(l);
                                    int lx = cursor + l * (LEAF_W + LEAF_GAP);
                                    leaf.dto.setTier("leaf");
                                    leaf.dto.setX(lx);
                                    leaf.dto.setY(LEAF_Y);
                                    leaf.dto.setW(LEAF_W);
                                    leaf.dto.setH(LEAF_H);

                                    var createdLeaf = roadmapService.createNode(teamId, leaf.dto);
                                    tempIdToRealId.put(leaf.tempId, createdLeaf.getId());

                                    // Edge mid -> leaf
                                    roadmapService.createEdge(teamId, RoadmapDto.RoadmapEdgeDto.builder()
                                            .from(createdMid.getId()).to(createdLeaf.getId()).build());
                                }

                                cursor += actualGroupW + GROUP_GAP;
                            }

                            // Root node coordinate & create
                            int totalW = Math.max(cursor - GROUP_GAP + PAD_X, 800);
                            int rx = (totalW / 2) - (ROOT_W / 2);
                            rootMeta.dto.setTier("root");
                            rootMeta.dto.setX(rx);
                            rootMeta.dto.setY(ROOT_Y);
                            rootMeta.dto.setW(ROOT_W);
                            rootMeta.dto.setH(ROOT_H);

                            var createdRoot = roadmapService.createNode(teamId, rootMeta.dto);
                            tempIdToRealId.put(rootMeta.tempId, createdRoot.getId());

                            // Edge root -> each mid
                            for (NodeMeta mid : midMetas) {
                                String midRealId = tempIdToRealId.get(mid.tempId);
                                if (midRealId != null) {
                                    roadmapService.createEdge(teamId, RoadmapDto.RoadmapEdgeDto.builder()
                                            .from(createdRoot.getId()).to(midRealId).build());
                                }
                            }

                            // Mark all suggestions as APPROVED
                            for (Suggestion currS : allForSource) {
                                if (!currS.getId().equals(s.getId())) {
                                    currS.setStatus("APPROVED");
                                    currS.setResolvedBy(approver);
                                    currS.setResolvedAt(Instant.now());
                                    suggestionRepository.save(currS);
                                }
                            }
                        } else {
                            // Fallback if allForSource is empty: create node for single suggestion
                        }
                    } else if (s.getTargetId() == null) {
                        // targetId is null and sourceId is null: create single node
                        RoadmapDto.RoadmapNodeDto singleDto = toNodeDtoFromJson(rootNode);
                        roadmapService.createNode(teamId, singleDto);
                    } else if (s.getTargetId() != null) {
                        Map<String, Object> changes = objectMapper.readValue(s.getChangeJson(), new TypeReference<>(){});
                        RoadmapDto.RoadmapNodeDto updateDto = new RoadmapDto.RoadmapNodeDto();
                        if (changes.containsKey("aiSummary")) updateDto.setAiSummary((String)changes.get("aiSummary"));
                        if (changes.containsKey("label")) updateDto.setLabel((String)changes.get("label"));
                        if (changes.containsKey("status")) updateDto.setStatus((String)changes.get("status"));
                        if (changes.containsKey("progress")) updateDto.setProgress(((Number)changes.get("progress")).intValue());
                        if (changes.containsKey("assignees")) updateDto.setAssignees(objectMapper.convertValue(changes.get("assignees"), new TypeReference<List<String>>(){}));
                        roadmapService.updateNode(teamId, s.getTargetId(), updateDto);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("failed applying suggestion change: " + e.getMessage(), e);
            }
        }

        s.setStatus("APPROVED");
        s.setResolvedBy(approver);
        s.setResolvedAt(Instant.now());
        Suggestion saved = suggestionRepository.save(s);
        return toDto(saved);
    }

    @Transactional
    public SuggestionDto.Response reject(String teamId, Long id, String approver) {
        Suggestion s = suggestionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("suggestion not found: " + id));
        if (!teamId.equals(s.getTeamId())) throw new IllegalArgumentException("team mismatch");
        if (!"PENDING".equals(s.getStatus())) throw new IllegalStateException("suggestion not pending");
        s.setStatus("REJECTED");
        s.setResolvedBy(approver);
        s.setResolvedAt(Instant.now());
        Suggestion saved = suggestionRepository.save(s);
        return toDto(saved);
    }

    public List<SuggestionDto.Response> getHistory(String teamId, Long suggestionId) {
        List<SuggestionDto.Response> history = new ArrayList<>();
        Long currentId = suggestionId;
        while (currentId != null) {
            final Long targetId = currentId;
            Optional<Suggestion> opt = suggestionRepository.findById(targetId)
                    .filter(s -> teamId.equals(s.getTeamId()));
            if (opt.isEmpty()) break;
            Suggestion current = opt.get();
            history.add(toDto(current));
            currentId = current.getParentSuggestionId();
            if (currentId != null && currentId.equals(targetId)) break; // cycle protection
        }
        return history;
    }

    public SuggestionDto.DiffResponse getDiff(String teamId, Long baseId, Long targetId) {
        Suggestion base = suggestionRepository.findById(baseId)
                .filter(s -> teamId.equals(s.getTeamId()))
                .orElseThrow(() -> new IllegalArgumentException("Base suggestion not found: " + baseId));

        Long actualTargetId = targetId != null ? targetId : base.getParentSuggestionId();
        if (actualTargetId == null) {
            return SuggestionDto.DiffResponse.builder()
                    .baseSuggestionId(baseId)
                    .targetSuggestionId(null)
                    .baseTitle(base.getTitle())
                    .targetTitle(null)
                    .titleChanged(false)
                    .baseBody(base.getBody())
                    .targetBody(null)
                    .bodyChanged(false)
                    .baseStatus(base.getStatus())
                    .targetStatus(null)
                    .statusChanged(false)
                    .changeJsonDiff("No parent/target suggestion to compare")
                    .build();
        }

        Suggestion target = suggestionRepository.findById(actualTargetId)
                .filter(s -> teamId.equals(s.getTeamId()))
                .orElseThrow(() -> new IllegalArgumentException("Target suggestion not found: " + actualTargetId));

        boolean titleChanged = !java.util.Objects.equals(base.getTitle(), target.getTitle());
        boolean bodyChanged = !java.util.Objects.equals(base.getBody(), target.getBody());
        boolean statusChanged = !java.util.Objects.equals(base.getStatus(), target.getStatus());

        String jsonDiff = "changeJson diff: ";
        if (java.util.Objects.equals(base.getChangeJson(), target.getChangeJson())) {
            jsonDiff += "No change in payload";
        } else {
            jsonDiff += "Base payload differs from target payload";
        }

        return SuggestionDto.DiffResponse.builder()
                .baseSuggestionId(baseId)
                .targetSuggestionId(actualTargetId)
                .baseTitle(base.getTitle())
                .targetTitle(target.getTitle())
                .titleChanged(titleChanged)
                .baseBody(base.getBody())
                .targetBody(target.getBody())
                .bodyChanged(bodyChanged)
                .baseStatus(base.getStatus())
                .targetStatus(target.getStatus())
                .statusChanged(statusChanged)
                .changeJsonDiff(jsonDiff)
                .build();
    }

    private SuggestionDto.Response toDto(Suggestion s) {
        return SuggestionDto.Response.builder()
                .id(s.getId())
                .teamId(s.getTeamId())
                .parentSuggestionId(s.getParentSuggestionId())
                .targetType(s.getTargetType())
                .targetId(s.getTargetId())
                .title(s.getTitle())
                .body(s.getBody())
                .sourceTool(s.getSourceTool())
                .sourceId(s.getSourceId())
                .changeJson(s.getChangeJson())
                .status(s.getStatus())
                .resolvedBy(s.getResolvedBy())
                .createdAt(s.getCreatedAt())
                .resolvedAt(s.getResolvedAt())
                .build();
    }
}
