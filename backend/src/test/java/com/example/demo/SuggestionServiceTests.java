package com.example.demo.service;

import com.example.demo.domain.RoadmapNode;
import com.example.demo.domain.Specification;
import com.example.demo.domain.Suggestion;
import com.example.demo.domain.Team;
import com.example.demo.dto.SuggestionDto;
import com.example.demo.repository.RoadmapNodeRepository;
import com.example.demo.repository.SpecificationRepository;
import com.example.demo.repository.SuggestionRepository;
import com.example.demo.repository.TeamRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SuggestionServiceTests {

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private AiService aiService;

    @Autowired
    private SuggestionRepository suggestionRepository;

    @Autowired
    private RoadmapNodeRepository roadmapNodeRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    public void approve_creates_roadmap_nodes_from_json_array() {
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

    @Test
    public void regenerate_origami_with_practice_feedback_produces_natural_wbs_modules() {
        String teamId = "T-ORIGAMI";
        teamRepository.save(Team.builder().teamId(teamId).name("OrigamiTeam").members(1).color("#ff5500").mission("종이학 접기").build());

        Specification spec = specificationRepository.save(Specification.builder()
                .teamId(teamId)
                .author("tester")
                .specText("종이학 접기")
                .status("READY")
                .build());

        SuggestionDto.Create base = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title("종이학 접기")
                .body("종이학 접기 목표")
                .sourceTool("test")
                .sourceId(String.valueOf(spec.getId()))
                .changeJson("{\"label\": \"종이학 접기\", \"aiSummary\": \"초기 생성\"}")
                .build();
        var original = suggestionService.create(teamId, base);

        var regenerated = suggestionService.regenerate(teamId, original.getId(), SuggestionDto.RegenerateRequest.builder()
                .feedback("다른 걸로 미리 연습하는 과정도 추가해줘")
                .baseSuggestionId(original.getId())
                .build());

        Assertions.assertFalse(regenerated.isEmpty());

        // 1. Must NOT have raw "피드백 반영:" in titles
        boolean hasRawPrefix = regenerated.stream().anyMatch(item -> item.getTitle() != null && item.getTitle().startsWith("피드백 반영:"));
        Assertions.assertFalse(hasRawPrefix, "Must not contain '피드백 반영:' prefix in node titles");

        // 2. Must contain naturally synthesized module for practice
        boolean hasPracticeModule = regenerated.stream().anyMatch(item -> item.getTitle() != null && (item.getTitle().contains("사전 연습") || item.getTitle().contains("예비 실습")));
        Assertions.assertTrue(hasPracticeModule, "Must contain synthesized practice/exercise module");
    }

    @Test
    public void generate_speaker_procurement_does_not_contain_software_development_tasks() {
        String teamId = "T-SPEAKER-1";
        teamRepository.save(Team.builder().teamId(teamId).name("SpeakerTeam").members(1).color("#10b981").mission("스피커 구매하기").build());

        Specification spec = specificationRepository.save(Specification.builder()
                .teamId(teamId)
                .author("tester")
                .specText("스피커 구매하기")
                .status("READY")
                .build());

        var suggestions = aiService.generateSuggestions(teamId, spec.getId(), spec.getSpecText());
        Assertions.assertFalse(suggestions.isEmpty());

        // Must NOT contain software dev concepts like API, DB, frontend, backend, or site development
        boolean hasSoftwareTerms = suggestions.stream().anyMatch(item -> {
            String title = item.getTitle() != null ? item.getTitle() : "";
            String body = item.getBody() != null ? item.getBody() : "";
            return title.contains("사이트") || title.contains("웹") || title.contains("API") || title.contains("데이터베이스")
                    || body.contains("프론트엔드") || body.contains("백엔드") || body.contains("RESTful") || body.contains("서버 배포");
        });
        Assertions.assertFalse(hasSoftwareTerms, "Speaker purchase spec must not generate software engineering tasks");

        // Must contain purchasing stages (요구 사양 / 모델 비교 / 판매처 선정 및 결제 / 검수)
        boolean hasPurchaseStage = suggestions.stream().anyMatch(item ->
                item.getTitle() != null && (item.getTitle().contains("요구") || item.getTitle().contains("비교") || item.getTitle().contains("판매처") || item.getTitle().contains("검수"))
        );
        Assertions.assertTrue(hasPurchaseStage, "Speaker purchase must contain domain-appropriate procurement stages");
    }

    @Test
    public void regenerate_speaker_with_cable_feedback_restructures_purchase_wbs_without_awkward_titles() {
        String teamId = "T-SPEAKER-2";
        teamRepository.save(Team.builder().teamId(teamId).name("SpeakerTeam2").members(1).color("#3b82f6").mission("스피커 구매하기").build());

        Specification spec = specificationRepository.save(Specification.builder()
                .teamId(teamId)
                .author("tester")
                .specText("스피커 구매하기")
                .status("READY")
                .build());

        SuggestionDto.Create base = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title("스피커 구매하기")
                .body("스피커 구매 명세")
                .sourceTool("test")
                .sourceId(String.valueOf(spec.getId()))
                .changeJson("{\"label\": \"스피커 구매하기\", \"aiSummary\": \"초기 생성\"}")
                .build();
        var original = suggestionService.create(teamId, base);

        var regenerated = suggestionService.regenerate(teamId, original.getId(), SuggestionDto.RegenerateRequest.builder()
                .feedback("스피커 선도 구매하고 싶어")
                .baseSuggestionId(original.getId())
                .build());

        Assertions.assertFalse(regenerated.isEmpty());

        // 1. Must NOT contain awkward conversational phrases like "스피커 선도 구매하고 싶어 구매 계획 및 실행"
        boolean hasAwkwardTitle = regenerated.stream().anyMatch(item ->
                item.getTitle() != null && (item.getTitle().contains("하고 싶어") || item.getTitle().contains("추가해줘") || item.getTitle().contains("해주세요"))
        );
        Assertions.assertFalse(hasAwkwardTitle, "Node titles must not contain awkward conversational feedback phrases");

        // 2. Must naturally integrate cable / accessory procurement into the WBS nodes
        boolean hasCableIntegration = regenerated.stream().anyMatch(item -> {
            String title = item.getTitle() != null ? item.getTitle() : "";
            String body = item.getBody() != null ? item.getBody() : "";
            return title.contains("케이블") || title.contains("선") || body.contains("케이블") || body.contains("단자");
        });
        Assertions.assertTrue(hasCableIntegration, "WBS nodes must be restructured to integrate cable requirements and inspection");
    }

    @Test
    public void generate_computer_parts_spec_does_not_contain_redundant_root_prefix() {
        String teamId = "T-PC-1";
        teamRepository.save(Team.builder().teamId(teamId).name("PcTeam").members(1).color("#8b5cf6").mission("컴퓨터 부품(SSD, RAM, CPU) 구매").build());

        Specification spec = specificationRepository.save(Specification.builder()
                .teamId(teamId)
                .author("tester")
                .specText("컴퓨터 부품(SSD, RAM, CPU) 구매")
                .status("READY")
                .build());

        var suggestions = aiService.generateSuggestions(teamId, spec.getId(), spec.getSpecText());
        Assertions.assertFalse(suggestions.isEmpty());

        // Mid and Leaf nodes must NOT have redundant prefix "컴퓨터 부품(SSD, RAM, CPU) 구매 -" or "중분류: 컴퓨터 부품(SSD, RAM, CPU) 구매"
        boolean hasRedundantPrefix = suggestions.stream().skip(1).anyMatch(item -> {
            String title = item.getTitle() != null ? item.getTitle() : "";
            return title.contains("컴퓨터 부품(SSD, RAM, CPU) 구매 -") || title.contains("중분류: 컴퓨터 부품(SSD, RAM, CPU) 구매");
        });
        Assertions.assertFalse(hasRedundantPrefix, "Sub-node titles must not prefix or repeat the root project name");
    }

    @Test
    public void regenerate_computer_parts_with_exclude_reviews_feedback_removes_review_nodes() {
        String teamId = "T-PC-2";
        teamRepository.save(Team.builder().teamId(teamId).name("PcTeam2").members(1).color("#6366f1").mission("컴퓨터 부품(SSD, RAM, CPU) 구매").build());

        Specification spec = specificationRepository.save(Specification.builder()
                .teamId(teamId)
                .author("tester")
                .specText("컴퓨터 부품(SSD, RAM, CPU) 구매")
                .status("READY")
                .build());

        SuggestionDto.Create base = SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title("컴퓨터 부품(SSD, RAM, CPU) 구매")
                .body("컴퓨터 부품 구매 초기 명세")
                .sourceTool("test")
                .sourceId(String.valueOf(spec.getId()))
                .changeJson("{\"label\": \"컴퓨터 부품 구매\", \"aiSummary\": \"초기 생성\"}")
                .build();
        var original = suggestionService.create(teamId, base);

        var regenerated = suggestionService.regenerate(teamId, original.getId(), SuggestionDto.RegenerateRequest.builder()
                .feedback("실구매자 리뷰는 빼줘")
                .baseSuggestionId(original.getId())
                .build());

        Assertions.assertFalse(regenerated.isEmpty());

        // Must NOT contain "실구매자 리뷰" or "리뷰" in any regenerated sub-node titles
        boolean hasReviewInSubNodes = regenerated.stream().skip(1).anyMatch(item -> {
            String title = item.getTitle() != null ? item.getTitle() : "";
            return title.contains("실구매자 리뷰") || title.contains("리뷰 및");
        });
        Assertions.assertFalse(hasReviewInSubNodes, "Regenerated suggestions must exclude review tasks when user asked to remove reviews");
    }
}
