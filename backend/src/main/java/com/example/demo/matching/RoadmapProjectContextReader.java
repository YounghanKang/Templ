package com.example.demo.matching;

import com.example.demo.domain.RoadmapEdge;
import com.example.demo.domain.RoadmapNode;
import com.example.demo.domain.Team;
import com.example.demo.repository.RoadmapEdgeRepository;
import com.example.demo.repository.RoadmapNodeRepository;
import com.example.demo.repository.TeamRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RoadmapProjectContextReader
        implements ProjectContextReader {

    private static final long DEFAULT_PROJECT_VERSION = 1L;

    private static final String DEPENDENCY_RELATION_TYPE =
            "DEPENDENCY";

    private final TeamRepository teamRepository;

    private final RoadmapNodeRepository roadmapNodeRepository;

    private final RoadmapEdgeRepository roadmapEdgeRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @Override
    public ProjectContext getProjectContext(
            String projectId
    ) {

        validateProjectId(projectId);

        Team team =
                teamRepository
                        .findByTeamId(projectId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "team not found: " + projectId
                                )
                        );

        List<NodeContext> nodes =
                roadmapNodeRepository
                        .findAllByTeamIdOrderByIdAsc(projectId)
                        .stream()
                        .map(this::toNodeContext)
                        .toList();

        return new ProjectContext(
                team.getTeamId(),
                team.getName(),
                team.getMission(),
                DEFAULT_PROJECT_VERSION,
                nodes
        );
    }


    @Override
    public List<NodeContext> findCandidateNodes(
            String projectId,
            String channelId
    ) {

        validateProjectId(projectId);

        if (!teamRepository.existsByTeamId(projectId)) {
            return List.of();
        }

        /*
         * 현재 BE1에는 Slack channel과 RoadmapNode의
         * 직접 매핑 정보가 없으므로 같은 Team의 전체 Node를
         * 1차 후보로 제공하고 BE2 CandidateNodeMatcher가
         * 관련도 순으로 Top 5를 추립니다.
         */
        return roadmapNodeRepository
                .findAllByTeamIdOrderByIdAsc(projectId)
                .stream()
                .map(this::toNodeContext)
                .toList();
    }


    @Override
    public List<NodeRelation> getRelatedNodes(
            String projectId,
            String nodeId
    ) {

        validateProjectId(projectId);

        if (nodeId == null || nodeId.isBlank()) {
            return List.of();
        }

        return roadmapEdgeRepository
                .findAllByTeamIdOrderByIdAsc(projectId)
                .stream()
                .filter(
                        edge ->
                                nodeId.equals(edge.getFromNodeId())
                                        ||
                                        nodeId.equals(edge.getToNodeId())
                )
                .map(this::toNodeRelation)
                .toList();
    }


    private NodeContext toNodeContext(
            RoadmapNode node
    ) {

        List<String> assignees =
                parseStringList(
                        node.getAssigneesJson()
                );

        List<String> prerequisites =
                parseStringList(
                        node.getPrerequisitesJson()
                );


        return new NodeContext(
                node.getNodeId(),
                node.getLabel(),
                node.getGoal(),
                node.getAiSummary(),
                node.getStatus(),
                node.getProgress(),
                assignees,
                prerequisites,
                node.getDueDate(),
                buildKeywords(
                        node,
                        assignees,
                        prerequisites
                )
        );
    }


    private NodeRelation toNodeRelation(
            RoadmapEdge edge
    ) {

        return new NodeRelation(
                edge.getFromNodeId(),
                edge.getToNodeId(),
                DEPENDENCY_RELATION_TYPE
        );
    }


    private List<String> buildKeywords(
            RoadmapNode node,
            List<String> assignees,
            List<String> prerequisites
    ) {

        Set<String> keywords =
                new LinkedHashSet<>();

        addKeyword(
                keywords,
                node.getCode()
        );

        addKeyword(
                keywords,
                node.getIssue()
        );

        addKeyword(
                keywords,
                node.getTier()
        );

        keywords.addAll(
                assignees
        );

        keywords.addAll(
                prerequisites
        );

        return new ArrayList<>(
                keywords
        );
    }

    private List<String> parseStringList(
            String json
    ) {

        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {

            return objectMapper.readValue(
                    json,
                    new TypeReference<List<String>>() {
                    }
            );

        } catch (Exception exception) {

            /*
             * BE1의 보조 JSON 필드가 깨져 있어도
             * 전체 AI 분석 파이프라인은 중단하지 않습니다.
             */
            return List.of();
        }
    }


    private void addKeyword(
            Set<String> keywords,
            String value
    ) {

        if (value != null && !value.isBlank()) {
            keywords.add(
                    value.trim()
            );
        }
    }


    private void validateProjectId(
            String projectId
    ) {

        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException(
                    "projectId는 필수입니다."
            );
        }
    }
}