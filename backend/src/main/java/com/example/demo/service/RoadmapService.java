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
        seedDefaultRoadmaps();
    }

    private void seedDefaultRoadmaps() {
        if (roadmapNodeRepository.count() > 0 || roadmapEdgeRepository.count() > 0) {
            return;
        }

        List<String> teamIds = teamRepository.findAllByOrderByIdAsc().stream()
                .map(Team::getTeamId)
                .toList();
        if (teamIds.isEmpty()) {
            teamRepository.saveAll(List.of(
                    Team.builder().teamId("T-001").name("Design System").members(8).color("#6b5cf6").mission("모든 사용자가 직관적으로 제품을 사용할 수 있도록 일관된 디자인 언어를 구축한다.").build(),
                    Team.builder().teamId("T-002").name("Platform Engineering").members(14).color("#10b981").mission("개발자 경험을 최우선으로, 확장 가능하고 안정적인 인프라 기반을 마련한다.").build(),
                    Team.builder().teamId("T-003").name("Growth & Marketing").members(6).color("#f59e0b").mission("데이터 기반 실험으로 제품 성장을 가속화하고 시장 점유율을 확대한다.").build()
            ));
            teamIds = teamRepository.findAllByOrderByIdAsc().stream().map(Team::getTeamId).toList();
        }

        for (String teamId : teamIds) {
            seedTeamGraph(teamId);
        }
    }

    private void seedTeamGraph(String teamId) {
        List<RoadmapNode> nodes = List.of(
                RoadmapNode.builder().nodeId("root").teamId(teamId).label("2D 게임 개발").code("GOAL").status("active").progress(42).goal("2D 액션 RPG 한 편을 기획부터 출시까지 완주한다.").dueDate("2025-12-31").assigneesJson(toJson(List.of("Alex Rivera", "Jordan Kim"))).prerequisitesJson(toJson(List.of())).aiSummary("전체 로드맵은 4개 축(디자인·엔진·스킬·보스)으로 구성되며 현재 디자인 축이 완료 단계, 엔진 축이 진행 중입니다.").issue(null).tier("root").x(280).y(22).w(240).h(74).filesJson(toJson(List.of(
                        new RoadmapDto.FileDto(1L, "2D_RPG_기획서_v3.pdf", "4.2 MB", "Alex Rivera", "2025-03-02", null),
                        new RoadmapDto.FileDto(2L, "전체_일정표.xlsx", "318 KB", "Jordan Kim", "2025-03-11", null)
                ))).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("m1").teamId(teamId).label("게임 디자인").code("P-01").status("done").progress(100).goal("세계관·스토리·레벨 구성을 확정하고 GDD를 완성한다.").dueDate("2025-03-31").assigneesJson(toJson(List.of("Sam Chen"))).prerequisitesJson(toJson(List.of())).aiSummary("GDD 초안이 완료되어 핵심 게임루프와 진행 시스템이 확정됐습니다.").issue(null).tier("mid").x(180).y(220).w(182).h(66).filesJson(toJson(List.of(
                        new RoadmapDto.FileDto(1L, "GDD_최종.docx", "1.8 MB", "Sam Chen", "2025-03-28", null),
                        new RoadmapDto.FileDto(2L, "레벨_레이아웃.fig", "12.4 MB", "Sam Chen", "2025-03-30", null)
                ))).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("m2").teamId(teamId).label("물리엔진").code("P-02").status("active").progress(55).goal("충돌·중력·이동을 포함한 물리 시뮬레이션 레이어를 구축한다.").dueDate("2025-05-15").assigneesJson(toJson(List.of("Morgan Lee", "Sam Chen"))).prerequisitesJson(toJson(List.of("게임 디자인"))).aiSummary("유니티 채택과 자체 개발 두 갈래를 병행 검토 중입니다.").issue("경사면 이동 시 캐릭터가 튀는 물리 버그가 재현되고 있습니다.").tier("mid").x(326).y(220).w(182).h(66).filesJson(toJson(List.of(
                        new RoadmapDto.FileDto(1L, "엔진_비교_리포트.pdf", "960 KB", "Morgan Lee", "2025-04-08", null)
                ))).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("m3").teamId(teamId).label("스킬 코딩").code("P-03").status("todo").progress(12).goal("검사·궁수·마법사 3개 직업의 스킬 시스템을 구현한다.").dueDate("2025-07-31").assigneesJson(toJson(List.of("Alex Rivera"))).prerequisitesJson(toJson(List.of("게임 디자인", "물리엔진"))).aiSummary("쿨타임·피격 판정·이펙트 연동이 물리 레이어에 강하게 의존합니다.").issue(null).tier("mid").x(472).y(220).w(182).h(66).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("m4").teamId(teamId).label("보스몹 코딩").code("P-04").status("todo").progress(0).goal("보스 AI 패턴과 페이즈 전환 시스템을 구현한다.").dueDate("2025-09-30").assigneesJson(toJson(List.of("Jordan Park", "Alex Rivera"))).prerequisitesJson(toJson(List.of("물리엔진", "스킬 코딩"))).aiSummary("오크·트롤 두 보스가 계획되어 있으며 각각 고유 패턴과 약점을 가집니다.").issue(null).tier("mid").x(618).y(220).w(182).h(66).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("l1").teamId(teamId).label("게임 장르").code("T-011").status("done").progress(100).goal("2D 액션 RPG로 장르를 확정하고 타깃 유저를 정의한다.").dueDate("2025-02-28").assigneesJson(toJson(List.of("Sam Chen"))).prerequisitesJson(toJson(List.of())).aiSummary("경쟁 타이틀 분석을 통해 2D 액션 RPG로 확정됐습니다.").issue(null).tier("leaf").x(60).y(396).w(128).h(58).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("l2").teamId(teamId).label("유니티").code("T-021").status("active").progress(70).goal("Unity 2022 LTS 기반 물리 환경을 세팅하고 검증한다.").dueDate("2025-04-01").assigneesJson(toJson(List.of("Morgan Lee"))).prerequisitesJson(toJson(List.of("게임 디자인"))).aiSummary("Rigidbody2D 이동과 Physics2D 충돌 레이어 구성이 대부분 완료됐습니다.").issue("경사면 이동 시 캐릭터가 튀는 물리 버그가 재현되고 있습니다.").tier("leaf").x(208).y(396).w(128).h(58).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("l3").teamId(teamId).label("자체 개발").code("T-022").status("todo").progress(25).goal("외부 엔진 없이 AABB 충돌·중력 물리 레이어를 구현한다.").dueDate("2025-06-30").assigneesJson(toJson(List.of("Sam Chen", "Morgan Lee"))).prerequisitesJson(toJson(List.of("게임 디자인"))).aiSummary("개념 증명 단계로 기본 충돌·중력만 동작합니다.").issue(null).tier("leaf").x(356).y(396).w(128).h(58).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("l4").teamId(teamId).label("검사").code("T-031").status("todo").progress(30).goal("강공격·막기·카운터로 구성된 근접 직업을 구현한다.").dueDate("2025-07-01").assigneesJson(toJson(List.of("Alex Rivera"))).prerequisitesJson(toJson(List.of("물리엔진"))).aiSummary("카운터 판정 프레임이 넓다는 리뷰가 있어 밸런스 기준값 확정이 선행되어야 합니다.").issue(null).tier("leaf").x(504).y(396).w(128).h(58).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("l5").teamId(teamId).label("궁수").code("T-032").status("todo").progress(10).goal("투사체 물리를 기반으로 원거리 직업을 구현한다.").dueDate("2025-07-15").assigneesJson(toJson(List.of("Alex Rivera"))).prerequisitesJson(toJson(List.of("물리엔진"))).aiSummary("화살 궤도와 충돌 처리가 물리 레이어에 직접 의존합니다.").issue(null).tier("leaf").x(652).y(396).w(128).h(58).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("l6").teamId(teamId).label("마법사").code("T-033").status("todo").progress(0).goal("광역 마법 3종과 VFX 연동을 구현한다.").dueDate("2025-08-01").assigneesJson(toJson(List.of("Jordan Park"))).prerequisitesJson(toJson(List.of("물리엔진"))).aiSummary("최저 방어력·최고 딜 포텐셜 구조로, 세 직업 중 밸런스 민감도가 가장 높습니다.").issue(null).tier("leaf").x(800).y(396).w(128).h(58).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("l7").teamId(teamId).label("오크").code("T-041").status("todo").progress(0).goal("1챕터 보스의 돌진·지진 패턴과 분노 페이즈를 구현한다.").dueDate("2025-08-31").assigneesJson(toJson(List.of("Jordan Park"))).prerequisitesJson(toJson(List.of("스킬 코딩"))).aiSummary("패턴 상태머신이 단순해 보스 AI 구조의 레퍼런스 역할을 합니다.").issue(null).tier("leaf").x(948).y(396).w(128).h(58).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build(),
                RoadmapNode.builder().nodeId("l8").teamId(teamId).label("트롤").code("T-042").status("todo").progress(0).goal("재생 능력과 약점 부위 판정을 갖춘 2챕터 보스를 구현한다.").dueDate("2025-09-15").assigneesJson(toJson(List.of("Alex Rivera", "Jordan Park"))).prerequisitesJson(toJson(List.of("스킬 코딩", "오크"))).aiSummary("재생 쿨타임이 짧을 경우 근접 직업의 클리어가 사실상 불가능하다는 테스트 결과가 있습니다.").issue(null).tier("leaf").x(1096).y(396).w(128).h(58).filesJson(toJson(List.of())).commentsJson(toJson(List.of())).build()
        );
        roadmapNodeRepository.saveAll(nodes);

        List<RoadmapEdge> edges = List.of(
                RoadmapEdge.builder().edgeId("e-root-m1").teamId(teamId).fromNodeId("root").toNodeId("m1").build(),
                RoadmapEdge.builder().edgeId("e-root-m2").teamId(teamId).fromNodeId("root").toNodeId("m2").build(),
                RoadmapEdge.builder().edgeId("e-root-m3").teamId(teamId).fromNodeId("root").toNodeId("m3").build(),
                RoadmapEdge.builder().edgeId("e-root-m4").teamId(teamId).fromNodeId("root").toNodeId("m4").build(),
                RoadmapEdge.builder().edgeId("e-m1-l1").teamId(teamId).fromNodeId("m1").toNodeId("l1").build(),
                RoadmapEdge.builder().edgeId("e-m2-l2").teamId(teamId).fromNodeId("m2").toNodeId("l2").build(),
                RoadmapEdge.builder().edgeId("e-m2-l3").teamId(teamId).fromNodeId("m2").toNodeId("l3").build(),
                RoadmapEdge.builder().edgeId("e-m3-l4").teamId(teamId).fromNodeId("m3").toNodeId("l4").build(),
                RoadmapEdge.builder().edgeId("e-m3-l5").teamId(teamId).fromNodeId("m3").toNodeId("l5").build(),
                RoadmapEdge.builder().edgeId("e-m3-l6").teamId(teamId).fromNodeId("m3").toNodeId("l6").build(),
                RoadmapEdge.builder().edgeId("e-m4-l7").teamId(teamId).fromNodeId("m4").toNodeId("l7").build(),
                RoadmapEdge.builder().edgeId("e-m4-l8").teamId(teamId).fromNodeId("m4").toNodeId("l8").build()
        );
        roadmapEdgeRepository.saveAll(edges);
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
        String id = request.getId() == null ? "n" + System.currentTimeMillis() : request.getId();
        RoadmapNode entity = RoadmapNode.builder()
                .nodeId(id)
                .teamId(teamId)
                .label(request.getLabel() == null ? "새 작업" : request.getLabel())
                .code(request.getCode() == null ? "T-900" : request.getCode())
                .status(request.getStatus() == null ? "todo" : request.getStatus())
                .progress(request.getProgress() == null ? 0 : request.getProgress())
                .goal(request.getGoal() == null ? "" : request.getGoal())
                .dueDate(request.getDueDate() == null ? "2025-12-31" : request.getDueDate())
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
