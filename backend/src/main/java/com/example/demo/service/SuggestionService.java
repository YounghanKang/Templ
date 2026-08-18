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
        if (item.has("aiSummary")) dto.setAiSummary(item.get("aiSummary").asText());
        else if (item.has("body")) dto.setAiSummary(item.get("body").asText());
        if (item.has("goal")) dto.setGoal(item.get("goal").asText());
        if (item.has("tier")) dto.setTier(item.get("tier").asText());
        if (item.has("status")) dto.setStatus(item.get("status").asText());
        if (item.has("progress")) dto.setProgress(item.get("progress").asInt());
        if (item.has("assignees") && item.get("assignees").isArray()) {
            java.util.List<String> asg = new java.util.ArrayList<>();
            item.get("assignees").forEach(n -> asg.add(n.asText()));
            dto.setAssignees(asg);
        }
        if (dto.getStatus() == null) dto.setStatus("todo");
        if (dto.getProgress() == null) dto.setProgress(0);
        if (dto.getAssignees() == null) dto.setAssignees(new java.util.ArrayList<>());
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

                            Map<String, String> tempIdToRealId = new java.util.HashMap<>();
                            Map<String, String> childToParentTempMap = new java.util.HashMap<>();
                            List<String> createdNodeIds = new java.util.ArrayList<>();

                            for (Suggestion currS : allForSource) {
                                JsonNode json = objectMapper.readTree(currS.getChangeJson());
                                RoadmapDto.RoadmapNodeDto nodeDto = toNodeDtoFromJson(json);
                                var createdNode = roadmapService.createNode(teamId, nodeDto);
                                createdNodeIds.add(createdNode.getId());

                                String tempId = json.has("tempId") ? json.get("tempId").asText() : null;
                                String parentTempId = json.has("parentTempId") ? json.get("parentTempId").asText() : null;

                                if (tempId != null && !tempId.isBlank()) {
                                    tempIdToRealId.put(tempId, createdNode.getId());
                                }
                                if (tempId != null && parentTempId != null && !parentTempId.isBlank()) {
                                    childToParentTempMap.put(tempId, parentTempId);
                                }

                                if (!currS.getId().equals(s.getId())) {
                                    currS.setStatus("APPROVED");
                                    currS.setResolvedBy(approver);
                                    currS.setResolvedAt(Instant.now());
                                    suggestionRepository.save(currS);
                                }
                            }

                            if (!childToParentTempMap.isEmpty()) {
                                for (Map.Entry<String, String> entry : childToParentTempMap.entrySet()) {
                                    String realChildId = tempIdToRealId.get(entry.getKey());
                                    String realParentId = tempIdToRealId.get(entry.getValue());
                                    if (realChildId != null && realParentId != null) {
                                        roadmapService.createEdge(teamId, RoadmapDto.RoadmapEdgeDto.builder()
                                                .from(realParentId).to(realChildId).build());
                                    }
                                }
                            } else if (createdNodeIds.size() > 1) {
                                String rootId = createdNodeIds.get(0);
                                for (int i = 1; i < createdNodeIds.size(); i++) {
                                    roadmapService.createEdge(teamId, RoadmapDto.RoadmapEdgeDto.builder()
                                            .from(rootId).to(createdNodeIds.get(i)).build());
                                }
                            }
                        } else {
                            // Fallback if allForSource is empty: create node for single suggestion
                            RoadmapDto.RoadmapNodeDto singleDto = toNodeDtoFromJson(rootNode);
                            roadmapService.createNode(teamId, singleDto);
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
