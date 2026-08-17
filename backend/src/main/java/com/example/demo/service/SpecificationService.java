package com.example.demo.service;

import com.example.demo.domain.Specification;
import com.example.demo.dto.SpecificationDto;
import com.example.demo.dto.SuggestionDto;
import com.example.demo.repository.SpecificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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

    @Transactional
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
        var suggestions = suggestionService.listForTeam(teamId).stream()
                .filter(s -> s.getSourceId() != null && s.getSourceId().equals(String.valueOf(specId)))
                .toList();

        List<com.example.demo.dto.RoadmapDto.RoadmapNodeDto> nodes = new java.util.ArrayList<>();
        List<com.example.demo.dto.RoadmapDto.RoadmapEdgeDto> edges = new java.util.ArrayList<>();

        // create a synthetic root node if there is a root suggestion (title contains '로['] or first suggestion)
        long idCounter = 1;
        String rootId = "preview-root-" + specId;

        if (!suggestions.isEmpty()) {
            var rootSuggestion = suggestions.get(0);
            com.example.demo.dto.RoadmapDto.RoadmapNodeDto rootNode = new com.example.demo.dto.RoadmapDto.RoadmapNodeDto();
            rootNode.setId(rootId);
            rootNode.setLabel(rootSuggestion.getTitle());
            rootNode.setAiSummary(extractAiSummary(rootSuggestion.getChangeJson()));
            rootNode.setStatus("todo");
            rootNode.setProgress(0);
            rootNode.setAssignees(new java.util.ArrayList<>());
            nodes.add(rootNode);

            for (int i = 1; i < suggestions.size(); i++) {
                var s = suggestions.get(i);
                String nid = "preview-" + specId + "-" + (idCounter++);
                com.example.demo.dto.RoadmapDto.RoadmapNodeDto node = new com.example.demo.dto.RoadmapDto.RoadmapNodeDto();
                node.setId(nid);
                node.setLabel(s.getTitle());
                node.setAiSummary(extractAiSummary(s.getChangeJson()));
                node.setStatus("todo");
                node.setProgress(0);
                node.setAssignees(extractAssignees(s.getChangeJson()));
                nodes.add(node);
                edges.add(com.example.demo.dto.RoadmapDto.RoadmapEdgeDto.builder().id("e-" + nid + "-" + rootId).from(rootId).to(nid).build());
            }
        }

        return com.example.demo.dto.RoadmapDto.RoadmapGraphDto.builder().nodes(nodes).edges(edges).build();
    }

    private String extractAiSummary(String changeJson) {
        if (changeJson == null) return "";
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = om.readTree(changeJson);
            if (node.has("aiSummary")) return node.get("aiSummary").asText("");
            if (node.has("summary")) return node.get("summary").asText("");
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private java.util.List<String> extractAssignees(String changeJson) {
        if (changeJson == null) return new java.util.ArrayList<>();
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = om.readTree(changeJson);
            if (node.has("assignees") && node.get("assignees").isArray()) {
                java.util.List<String> out = new java.util.ArrayList<>();
                for (var it : node.get("assignees")) out.add(it.asText());
                return out;
            }
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
}
