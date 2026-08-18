package com.example.demo.service;

import com.example.demo.domain.RoadmapEdge;
import com.example.demo.domain.RoadmapNode;
import com.example.demo.domain.Team;
import com.example.demo.dto.RoadmapDto;
import com.example.demo.repository.RoadmapEdgeRepository;
import com.example.demo.repository.RoadmapNodeRepository;
import com.example.demo.repository.TeamRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoadmapService {

    private final TeamRepository teamRepository;
    private final RoadmapNodeRepository roadmapNodeRepository;
    private final RoadmapEdgeRepository roadmapEdgeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoadmapService(TeamRepository teamRepository,
                         RoadmapNodeRepository roadmapNodeRepository,
                         RoadmapEdgeRepository roadmapEdgeRepository) {
        this.teamRepository = teamRepository;
        this.roadmapNodeRepository = roadmapNodeRepository;
        this.roadmapEdgeRepository = roadmapEdgeRepository;
    }



    public RoadmapDto.RoadmapGraphDto getRoadmapGraph(String teamId) {
        List<RoadmapNode> nodeEntities = roadmapNodeRepository.findAllByTeamIdOrderByIdAsc(teamId);
        List<RoadmapEdge> edgeEntities = roadmapEdgeRepository.findAllByTeamIdOrderByIdAsc(teamId);
        return RoadmapDto.RoadmapGraphDto.builder()
                .nodes(nodeEntities.stream().map(this::toDto).toList())
                .edges(edgeEntities.stream().map(this::toEdgeDto).toList())
                .build();
    }

    public RoadmapDto.RoadmapNodeDto createNode(String teamId, RoadmapDto.RoadmapNodeDto request) {
        teamRepository.findByTeamId(teamId).orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        String id = request.getId() == null ? "n" + java.util.UUID.randomUUID().toString() : request.getId();
        RoadmapNode entity = RoadmapNode.builder()
                .nodeId(id)
                .teamId(teamId)
                .label(request.getLabel() == null ? "새 작업" : request.getLabel())
                .code(request.getCode() == null ? "T-900" : request.getCode())
                .status(request.getStatus() == null ? "todo" : request.getStatus())
                .progress(request.getProgress() == null ? 0 : request.getProgress())
                .goal(request.getGoal() == null ? "" : request.getGoal())
                .dueDate(request.getDueDate() == null ? java.time.LocalDate.now().plusMonths(1).toString() : request.getDueDate())
                .assigneesJson(toJson(request.getAssignees() == null ? new ArrayList<>() : request.getAssignees()))
                .prerequisitesJson(toJson(request.getPrerequisites() == null ? new ArrayList<>() : request.getPrerequisites()))
                .aiSummary(request.getAiSummary() == null ? "" : request.getAiSummary())
                .issue(request.getIssue())
                .tier(request.getTier() == null ? "leaf" : request.getTier())
                .x(request.getX() == null ? 60 : request.getX())
                .y(request.getY() == null ? 430 : request.getY())
                .w(request.getW() == null ? 128 : request.getW())
                .h(request.getH() == null ? 58 : request.getH())
                .filesJson(toJson(request.getFiles() == null ? new ArrayList<>() : request.getFiles()))
                .commentsJson(toJson(request.getComments() == null ? new ArrayList<>() : request.getComments()))
                .build();
        return toDto(roadmapNodeRepository.save(entity));
    }

    public RoadmapDto.RoadmapNodeDto updateNode(String teamId, String nodeId, RoadmapDto.RoadmapNodeDto request) {
        RoadmapNode node = roadmapNodeRepository.findByNodeIdAndTeamId(nodeId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("node not found: " + nodeId));
        if (request.getLabel() != null) node.setLabel(request.getLabel());
        if (request.getCode() != null) node.setCode(request.getCode());
        if (request.getStatus() != null) node.setStatus(request.getStatus());
        if (request.getProgress() != null) node.setProgress(request.getProgress());
        if (request.getGoal() != null) node.setGoal(request.getGoal());
        if (request.getDueDate() != null) node.setDueDate(request.getDueDate());
        if (request.getAssignees() != null) node.setAssigneesJson(toJson(request.getAssignees()));
        if (request.getPrerequisites() != null) node.setPrerequisitesJson(toJson(request.getPrerequisites()));
        if (request.getAiSummary() != null) node.setAiSummary(request.getAiSummary());
        if (request.getIssue() != null) node.setIssue(request.getIssue());
        if (request.getFiles() != null) node.setFilesJson(toJson(request.getFiles()));
        if (request.getComments() != null) node.setCommentsJson(toJson(request.getComments()));
        if (request.getTier() != null) node.setTier(request.getTier());
        if (request.getX() != null) node.setX(request.getX());
        if (request.getY() != null) node.setY(request.getY());
        if (request.getW() != null) node.setW(request.getW());
        if (request.getH() != null) node.setH(request.getH());
        return toDto(roadmapNodeRepository.save(node));
    }

    public void deleteNode(String teamId, String nodeId) {
        roadmapNodeRepository.deleteByNodeIdAndTeamId(nodeId, teamId);
        roadmapEdgeRepository.findAllByTeamIdOrderByIdAsc(teamId).stream()
                .filter(edge -> edge.getFromNodeId().equals(nodeId) || edge.getToNodeId().equals(nodeId))
                .forEach(edge -> roadmapEdgeRepository.deleteByEdgeIdAndTeamId(edge.getEdgeId(), teamId));
    }

    public RoadmapDto.RoadmapEdgeDto createEdge(String teamId, RoadmapDto.RoadmapEdgeDto request) {
        teamRepository.findByTeamId(teamId).orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        String id = request.getId() == null ? "e-" + request.getFrom() + "-" + request.getTo() : request.getId();
        RoadmapEdge edge = RoadmapEdge.builder()
                .edgeId(id)
                .teamId(teamId)
                .fromNodeId(request.getFrom())
                .toNodeId(request.getTo())
                .build();
        return toEdgeDto(roadmapEdgeRepository.save(edge));
    }

    public void deleteEdge(String teamId, String edgeId) {
        roadmapEdgeRepository.deleteByEdgeIdAndTeamId(edgeId, teamId);
    }

    private RoadmapDto.RoadmapNodeDto toDto(RoadmapNode entity) {
        return RoadmapDto.RoadmapNodeDto.builder()
                .id(entity.getNodeId())
                .label(entity.getLabel())
                .code(entity.getCode())
                .status(entity.getStatus())
                .progress(entity.getProgress())
                .goal(entity.getGoal())
                .dueDate(entity.getDueDate())
                .assignees(fromJsonList(entity.getAssigneesJson(), String.class))
                .prerequisites(fromJsonList(entity.getPrerequisitesJson(), String.class))
                .aiSummary(entity.getAiSummary())
                .issue(entity.getIssue())
                .files(fromJsonList(entity.getFilesJson(), RoadmapDto.FileDto.class))
                .tier(entity.getTier())
                .x(entity.getX())
                .y(entity.getY())
                .w(entity.getW())
                .h(entity.getH())
                .comments(fromJsonList(entity.getCommentsJson(), RoadmapDto.CommentDto.class))
                .build();
    }

    private RoadmapDto.RoadmapEdgeDto toEdgeDto(RoadmapEdge entity) {
        return RoadmapDto.RoadmapEdgeDto.builder()
                .id(entity.getEdgeId())
                .from(entity.getFromNodeId())
                .to(entity.getToNodeId())
                .build();
    }

    private <T> List<T> fromJsonList(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse JSON for roadmap field", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new ArrayList<>() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize roadmap field", e);
        }
    }
}
