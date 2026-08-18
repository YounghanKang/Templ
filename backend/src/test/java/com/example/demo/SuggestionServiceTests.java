package com.example.demo;

import com.example.demo.dto.RoadmapDto;
import com.example.demo.dto.SuggestionDto;
import com.example.demo.dto.SpecificationDto;
import com.example.demo.domain.Specification;
import com.example.demo.domain.Team;
import com.example.demo.repository.RoadmapNodeRepository;
import com.example.demo.repository.SpecificationRepository;
import com.example.demo.repository.TeamRepository;
import com.example.demo.service.RoadmapService;
import com.example.demo.service.SuggestionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
public class SuggestionServiceTests {

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private RoadmapService roadmapService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RoadmapNodeRepository roadmapNodeRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Test
    public void approve_updates_existing_node_fields() {
        String teamId = "T-TEST-1";
        teamRepository.save(Team.builder().teamId(teamId).name("TestTeam").members(1).color("#000").mission("m").build());

        RoadmapDto.RoadmapNodeDto node = RoadmapDto.RoadmapNodeDto.builder()
                .id("n-root-1")
                .label("Initial")
                .aiSummary("")
                .status("todo")
                .progress(0)
                .assignees(List.of())
                .build();
        roadmapService.createNode(teamId, node);

        String changeJson = "{\"label\":\"Updated Label\",\"aiSummary\":\"Updated summary\",\"progress\":50}";
        SuggestionDto.Create create = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId("n-root-1")
                .title("Apply update")
                .body("Apply field updates")
                .sourceTool("test")
                .sourceId("spec-1")
                .changeJson(changeJson)
                .build();
        var s = suggestionService.create(teamId, create);
        var approved = suggestionService.approve(teamId, s.getId(), "tester");

        var updated = roadmapNodeRepository.findByNodeIdAndTeamId("n-root-1", teamId).orElseThrow();
        Assertions.assertEquals("Updated Label", updated.getLabel());
        Assertions.assertEquals("Updated summary", updated.getAiSummary());
        Assertions.assertEquals(50, updated.getProgress());
    }

    @Test
    public void approve_creates_root_and_children_from_array() {
        String teamId = "T-TEST-2";
        teamRepository.save(Team.builder().teamId(teamId).name("Team2").members(1).color("#111").mission("m").build());

        String multiJson = "[\n" +
                "  { \"title\": \"Root Generated\", \"aiSummary\": \"root summary\" },\n" +
                "  { \"title\": \"Child A\", \"aiSummary\": \"child a\" },\n" +
                "  { \"title\": \"Child B\", \"aiSummary\": \"child b\", \"assignees\": [\"Alice\"] }\n" +
                "]";

        SuggestionDto.Create create = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title("Create tree")
                .body("Create root+children")
                .sourceTool("test")
                .sourceId("spec-2")
                .changeJson(multiJson)
                .build();
        var s = suggestionService.create(teamId, create);
        suggestionService.approve(teamId, s.getId(), "tester");

        var nodes = roadmapNodeRepository.findAllByTeamIdOrderByIdAsc(teamId);
        Assertions.assertTrue(nodes.size() >= 3, "expected at least 3 nodes, got " + nodes.size());
        boolean foundRoot = nodes.stream().anyMatch(n -> "Root Generated".equals(n.getLabel()));
        boolean foundChildA = nodes.stream().anyMatch(n -> "Child A".equals(n.getLabel()));
        boolean foundChildB = nodes.stream().anyMatch(n -> "Child B".equals(n.getLabel()));
        Assertions.assertTrue(foundRoot && foundChildA && foundChildB, "Expected root and children to exist");
    }

    @Test
    public void regenerate_uses_user_feedback_for_revised_suggestions() {
        String teamId = "T-TEST-3";
        teamRepository.save(Team.builder().teamId(teamId).name("Team3").members(1).color("#222").mission("m").build());

        Specification spec = specificationRepository.save(Specification.builder()
                .teamId(teamId)
                .author("tester")
                .specText("로그인 API 구현, 사용자 관리 기능, 관리자 대시보드 개발")
                .status("READY")
                .build());

        SuggestionDto.Create base = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title("기본 제안")
                .body("초기 제안")
                .sourceTool("test")
                .sourceId(String.valueOf(spec.getId()))
                .changeJson("{\"label\": \"로그인 API\", \"aiSummary\": \"초기 생성\"}")
                .build();
        var original = suggestionService.create(teamId, base);

        var regenerated = suggestionService.regenerate(teamId, original.getId(), SuggestionDto.RegenerateRequest.builder()
                .feedback("관리자 대시보드는 제외하고 로그인만 우선 구현해줘")
                .baseSuggestionId(original.getId())
                .build());

        Assertions.assertFalse(regenerated.isEmpty());
        Assertions.assertTrue(regenerated.stream().anyMatch(item ->
                item.getBody() != null && item.getBody().contains("관리자 대시보드는 제외하고 로그인만 우선 구현해줘")
        ));
    }

    @Test
    public void approve_creates_single_node_when_target_id_is_null() {
        String teamId = "T-TEST-4";
        teamRepository.save(Team.builder().teamId(teamId).name("Team4").members(1).color("#333").mission("m").build());

        SuggestionDto.Create single = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title("Single task")
                .body("Single task body")
                .sourceTool("test")
                .sourceId(null)
                .changeJson("{\"label\": \"독립 노드\", \"aiSummary\": \"단일 노드 요약\"}")
                .build();
        var s = suggestionService.create(teamId, single);
        suggestionService.approve(teamId, s.getId(), "tester");

        var nodes = roadmapNodeRepository.findAllByTeamIdOrderByIdAsc(teamId);
        Assertions.assertTrue(nodes.stream().anyMatch(n -> "독립 노드".equals(n.getLabel())));
    }

    @Test
    public void test_parent_suggestion_id_history_and_diff() {
        String teamId = "T-TEST-5";
        teamRepository.save(Team.builder().teamId(teamId).name("Team5").members(1).color("#444").mission("m").build());

        Specification spec = specificationRepository.save(Specification.builder()
                .teamId(teamId)
                .author("tester")
                .specText("기능 명세서 버전 1")
                .status("READY")
                .build());

        SuggestionDto.Create parent = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title("부모 제안")
                .body("부모 설명")
                .sourceTool("test")
                .sourceId(String.valueOf(spec.getId()))
                .changeJson("{\"label\": \"부모\", \"aiSummary\": \"v1\"}")
                .build();
        var parentRes = suggestionService.create(teamId, parent);

        var regeneratedList = suggestionService.regenerate(teamId, parentRes.getId(), SuggestionDto.RegenerateRequest.builder()
                .feedback("피드백 반영 버전 2")
                .baseSuggestionId(parentRes.getId())
                .build());

        Assertions.assertFalse(regeneratedList.isEmpty());
        var childRes = regeneratedList.get(0);
        Assertions.assertEquals(parentRes.getId(), childRes.getParentSuggestionId());

        // Test History
        var history = suggestionService.getHistory(teamId, childRes.getId());
        Assertions.assertEquals(2, history.size());
        Assertions.assertEquals(childRes.getId(), history.get(0).getId());
        Assertions.assertEquals(parentRes.getId(), history.get(1).getId());

        // Test Diff
        var diff = suggestionService.getDiff(teamId, childRes.getId(), parentRes.getId());
        Assertions.assertEquals(childRes.getId(), diff.getBaseSuggestionId());
        Assertions.assertEquals(parentRes.getId(), diff.getTargetSuggestionId());
        Assertions.assertTrue(diff.isTitleChanged() || diff.isBodyChanged());
    }
}
