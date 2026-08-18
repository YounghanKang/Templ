package com.example.demo.service;

import com.example.demo.domain.Specification;
import com.example.demo.dto.SpecificationDto;
import com.example.demo.dto.SuggestionDto;
import com.example.demo.repository.SpecificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpecificationService {

    private final SpecificationRepository specificationRepository;
    private final AiService aiService;
    private final SuggestionService suggestionService;
    private final AsyncSpecificationProcessor asyncProcessor;

    public SpecificationService(SpecificationRepository specificationRepository, AiService aiService, SuggestionService suggestionService, AsyncSpecificationProcessor asyncProcessor) {
        this.specificationRepository = specificationRepository;
        this.aiService = aiService;
        this.suggestionService = suggestionService;
        this.asyncProcessor = asyncProcessor;
    }

    public SpecificationDto.Response submit(String teamId, SpecificationDto.Create req) {
        Specification spec = Specification.builder()
                .teamId(teamId)
                .author(req.getAuthor() == null ? "anonymous" : req.getAuthor())
                .specText(req.getSpecText() == null ? "" : req.getSpecText())
                .status("ANALYZING")
                .createdAt(Instant.now())
                .build();
        Specification saved = specificationRepository.save(spec);

        // enqueue async processing
        spec = saved; // use saved instance
        // status remains ANALYZING until async processor updates it
        // call async processor
        // kick off async processing
        try {
            asyncProcessor.processSpecification(saved.getId());
        } catch (Exception e) {
            // log but do not fail the request
            // simple stdout logger to avoid adding SLF4J here
            System.err.println("Failed to start async processor for spec " + saved.getId() + ": " + e.getMessage());
        }

        return SpecificationDto.Response.builder()
                .id(saved.getId())
                .teamId(saved.getTeamId())
                .author(saved.getAuthor())
                .specText(saved.getSpecText())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<SpecificationDto.Response> listForTeam(String teamId) {
        return specificationRepository.findAllByTeamIdOrderByIdDesc(teamId).stream().map(s -> SpecificationDto.Response.builder()
                .id(s.getId())
                .teamId(s.getTeamId())
                .author(s.getAuthor())
                .specText(s.getSpecText())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    /**
     * Build a preview roadmap graph from suggestions linked to a spec without persisting nodes.
     */
    public com.example.demo.dto.RoadmapDto.RoadmapGraphDto generatePreviewGraph(String teamId, Long specId) {
        var rawSuggestions = suggestionService.listForTeam(teamId).stream()
                .filter(s -> s.getSourceId() != null && s.getSourceId().equals(String.valueOf(specId)) && "PENDING".equals(s.getStatus()))
                .sorted(java.util.Comparator.comparing(SuggestionDto.Response::getId))
                .toList();

        if (rawSuggestions.isEmpty()) {
            return com.example.demo.dto.RoadmapDto.RoadmapGraphDto.builder()
                    .nodes(new java.util.ArrayList<>())
                    .edges(new java.util.ArrayList<>())
                    .build();
        }

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

        // 1. Parse nodes metadata
        List<PreviewNodeMeta> metas = new java.util.ArrayList<>();
        for (int i = 0; i < rawSuggestions.size(); i++) {
            var s = rawSuggestions.get(i);
            String previewId = "preview-" + specId + "-" + (i + 1);
            String tempId = "node_" + (i + 1);
            String parentTempId = null;
            String tier = (i == 0) ? "root" : "leaf";
            String label = s.getTitle();
            String code = String.format("T-%03d", i + 1);
            String goal = s.getBody();
            String dueDate = java.time.LocalDate.now().plusMonths(1).toString();
            List<String> assignees = new java.util.ArrayList<>();
            String aiSummary = "";

            if (s.getChangeJson() != null && !s.getChangeJson().isBlank()) {
                try {
                    var json = om.readTree(s.getChangeJson());
                    if (json.has("tempId") && !json.get("tempId").isNull()) tempId = json.get("tempId").asText();
                    if (json.has("parentTempId") && !json.get("parentTempId").isNull()) parentTempId = json.get("parentTempId").asText();
                    if (json.has("tier")) tier = json.get("tier").asText();
                    if (json.has("label")) label = json.get("label").asText();
                    if (json.has("code")) code = json.get("code").asText();
                    if (json.has("goal")) goal = json.get("goal").asText();
                    if (json.has("dueDate")) dueDate = json.get("dueDate").asText();
                    if (json.has("aiSummary")) aiSummary = json.get("aiSummary").asText();
                    if (json.has("assignees") && json.get("assignees").isArray()) {
                        for (var item : json.get("assignees")) assignees.add(item.asText());
                    }
                } catch (Exception ignored) {}
            }

            metas.add(new PreviewNodeMeta(previewId, tempId, parentTempId, tier, label, code, goal, dueDate, assignees, aiSummary, s.getId()));
        }

        // 2. Classify hierarchy
        PreviewNodeMeta rootMeta = metas.stream().filter(m -> "root".equalsIgnoreCase(m.tier) || m.parentTempId == null).findFirst().orElse(metas.get(0));
        List<PreviewNodeMeta> midMetas = metas.stream().filter(m -> m != rootMeta && ("mid".equalsIgnoreCase(m.tier) || (rootMeta.tempId.equals(m.parentTempId)))).toList();
        if (midMetas.isEmpty() && metas.size() > 1) {
            midMetas = metas.subList(1, Math.min(metas.size(), 4));
        }

        final List<PreviewNodeMeta> finalMidMetas = midMetas;
        List<PreviewNodeMeta> leafMetas = metas.stream().filter(m -> m != rootMeta && !finalMidMetas.contains(m)).toList();

        // 3. Compute coordinates
        int LEAF_W = 128, LEAF_H = 58, MID_W = 182, MID_H = 66, ROOT_W = 240, ROOT_H = 74;
        int PAD_X = 46, GROUP_GAP = 46, LEAF_GAP = 16;
        int ROOT_Y = 22, MID_Y = 216, LEAF_Y = 396;

        int cursor = PAD_X;
        List<com.example.demo.dto.RoadmapDto.RoadmapNodeDto> outNodes = new java.util.ArrayList<>();
        List<com.example.demo.dto.RoadmapDto.RoadmapEdgeDto> outEdges = new java.util.ArrayList<>();
        Map<String, String> tempIdToPreviewId = new java.util.HashMap<>();

        tempIdToPreviewId.put(rootMeta.tempId, rootMeta.previewId);

        // Position mid modules & their leaf children
        for (int m = 0; m < midMetas.size(); m++) {
            PreviewNodeMeta mid = midMetas.get(m);
            tempIdToPreviewId.put(mid.tempId, mid.previewId);

            List<PreviewNodeMeta> children = leafMetas.stream()
                    .filter(l -> mid.tempId.equals(l.parentTempId))
                    .toList();
            if (children.isEmpty() && !leafMetas.isEmpty()) {
                // distribute proportionally if parentTempId was unspecified
                int perMid = Math.max(1, leafMetas.size() / midMetas.size());
                int start = m * perMid;
                int end = (m == midMetas.size() - 1) ? leafMetas.size() : Math.min(leafMetas.size(), (m + 1) * perMid);
                if (start < leafMetas.size()) {
                    children = leafMetas.subList(start, end);
                }
            }

            int groupW = children.isEmpty() ? MID_W : children.size() * LEAF_W + (children.size() - 1) * LEAF_GAP;
            int actualGroupW = Math.max(groupW, MID_W);

            // Add leaves
            for (int l = 0; l < children.size(); l++) {
                PreviewNodeMeta leaf = children.get(l);
                tempIdToPreviewId.put(leaf.tempId, leaf.previewId);
                int lx = cursor + l * (LEAF_W + LEAF_GAP);
                int ly = LEAF_Y;

                outNodes.add(com.example.demo.dto.RoadmapDto.RoadmapNodeDto.builder()
                        .id(leaf.previewId)
                        .label(leaf.label)
                        .code(leaf.code)
                        .tier("leaf")
                        .goal(leaf.goal)
                        .dueDate(leaf.dueDate)
                        .assignees(leaf.assignees)
                        .aiSummary(leaf.aiSummary)
                        .status("todo")
                        .progress(0)
                        .x(lx).y(ly).w(LEAF_W).h(LEAF_H)
                        .build());

                outEdges.add(com.example.demo.dto.RoadmapDto.RoadmapEdgeDto.builder()
                        .id("e-" + mid.previewId + "-" + leaf.previewId)
                        .from(mid.previewId)
                        .to(leaf.previewId)
                        .build());
            }

            // Add mid node
            int mx = cursor + (actualGroupW / 2) - (MID_W / 2);
            int my = MID_Y;
            outNodes.add(com.example.demo.dto.RoadmapDto.RoadmapNodeDto.builder()
                    .id(mid.previewId)
                    .label(mid.label)
                    .code(mid.code)
                    .tier("mid")
                    .goal(mid.goal)
                    .dueDate(mid.dueDate)
                    .assignees(mid.assignees)
                    .aiSummary(mid.aiSummary)
                    .status("todo")
                    .progress(0)
                    .x(mx).y(my).w(MID_W).h(MID_H)
                    .build());

            outEdges.add(com.example.demo.dto.RoadmapDto.RoadmapEdgeDto.builder()
                    .id("e-" + rootMeta.previewId + "-" + mid.previewId)
                    .from(rootMeta.previewId)
                    .to(mid.previewId)
                    .build());

            cursor += actualGroupW + GROUP_GAP;
        }

        // Add root node
        int totalW = Math.max(cursor - GROUP_GAP + PAD_X, 800);
        int rx = (totalW / 2) - (ROOT_W / 2);
        outNodes.add(0, com.example.demo.dto.RoadmapDto.RoadmapNodeDto.builder()
                .id(rootMeta.previewId)
                .label(rootMeta.label)
                .code(rootMeta.code)
                .tier("root")
                .goal(rootMeta.goal)
                .dueDate(rootMeta.dueDate)
                .assignees(rootMeta.assignees)
                .aiSummary(rootMeta.aiSummary)
                .status("todo")
                .progress(0)
                .x(rx).y(ROOT_Y).w(ROOT_W).h(ROOT_H)
                .build());

        return com.example.demo.dto.RoadmapDto.RoadmapGraphDto.builder().nodes(outNodes).edges(outEdges).build();
    }

    private static class PreviewNodeMeta {
        String previewId;
        String tempId;
        String parentTempId;
        String tier;
        String label;
        String code;
        String goal;
        String dueDate;
        List<String> assignees;
        String aiSummary;
        Long suggestionId;

        PreviewNodeMeta(String previewId, String tempId, String parentTempId, String tier, String label, String code, String goal, String dueDate, List<String> assignees, String aiSummary, Long suggestionId) {
            this.previewId = previewId;
            this.tempId = tempId;
            this.parentTempId = parentTempId;
            this.tier = tier;
            this.label = label;
            this.code = code;
            this.goal = goal;
            this.dueDate = dueDate;
            this.assignees = assignees;
            this.aiSummary = aiSummary;
            this.suggestionId = suggestionId;
        }
    }
}
