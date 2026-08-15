package com.example.demo.service;

import com.example.demo.dto.RoadmapDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoadmapService {

    private final Map<String, RoadmapDto.RoadmapNodeDto> nodes = new LinkedHashMap<>();
    private final List<RoadmapDto.RoadmapEdgeDto> edges = new ArrayList<>();

    public RoadmapService() {
        seed();
    }

    private void seed() {
        addNode(new RoadmapDto.RoadmapNodeDto("root", "2D 게임 개발", "GOAL", "active", 42,
                "2D 액션 RPG 한 편을 기획부터 출시까지 완주한다.", "2025-12-31",
                List.of("Alex Rivera", "Jordan Kim"), List.of(),
                "전체 로드맵은 4개 축(디자인·엔진·스킬·보스)으로 구성되며 현재 디자인 축이 완료 단계, 엔진 축이 진행 중입니다.",
                null, List.of(
                        new RoadmapDto.FileDto(1L, "2D_RPG_기획서_v3.pdf", "4.2 MB", "Alex Rivera", "2025-03-02", null),
                        new RoadmapDto.FileDto(2L, "전체_일정표.xlsx", "318 KB", "Jordan Kim", "2025-03-11", null)
                ), "root", 280, 22, 240, 74, new ArrayList<>()));

        addNode(new RoadmapDto.RoadmapNodeDto("m1", "게임 디자인", "P-01", "done", 100,
                "세계관·스토리·레벨 구성을 확정하고 GDD를 완성한다.", "2025-03-31",
                List.of("Sam Chen"), List.of(),
                "GDD 초안이 완료되어 핵심 게임루프와 진행 시스템이 확정됐습니다.",
                null, List.of(
                        new RoadmapDto.FileDto(1L, "GDD_최종.docx", "1.8 MB", "Sam Chen", "2025-03-28", null),
                        new RoadmapDto.FileDto(2L, "레벨_레이아웃.fig", "12.4 MB", "Sam Chen", "2025-03-30", null)
                ), "mid", 180, 220, 182, 66, new ArrayList<>()));

        addNode(new RoadmapDto.RoadmapNodeDto("m2", "물리엔진", "P-02", "active", 55,
                "충돌·중력·이동을 포함한 물리 시뮬레이션 레이어를 구축한다.", "2025-05-15",
                List.of("Morgan Lee", "Sam Chen"), List.of("게임 디자인"),
                "유니티 채택과 자체 개발 두 갈래를 병행 검토 중입니다.",
                "경사면 이동 시 캐릭터가 튀는 물리 버그가 재현되고 있습니다.", List.of(
                        new RoadmapDto.FileDto(1L, "엔진_비교_리포트.pdf", "960 KB", "Morgan Lee", "2025-04-08", null)
                ), "mid", 326, 220, 182, 66, new ArrayList<>()));

        addNode(new RoadmapDto.RoadmapNodeDto("m3", "스킬 코딩", "P-03", "todo", 12,
                "검사·궁수·마법사 3개 직업의 스킬 시스템을 구현한다.", "2025-07-31",
                List.of("Alex Rivera"), List.of("게임 디자인", "물리엔진"),
                "쿨타임·피격 판정·이펙트 연동이 물리 레이어에 강하게 의존합니다.", null, List.of(), "mid", 472, 220, 182, 66, new ArrayList<>()));

        addNode(new RoadmapDto.RoadmapNodeDto("m4", "보스몹 코딩", "P-04", "todo", 0,
                "보스 AI 패턴과 페이즈 전환 시스템을 구현한다.", "2025-09-30",
                List.of("Jordan Park", "Alex Rivera"), List.of("물리엔진", "스킬 코딩"),
                "오크·트롤 두 보스가 계획되어 있으며 각각 고유 패턴과 약점을 가집니다.", null, List.of(), "mid", 618, 220, 182, 66, new ArrayList<>()));

        addNode(new RoadmapDto.RoadmapNodeDto("l1", "게임 장르", "T-011", "done", 100,
                "2D 액션 RPG로 장르를 확정하고 타깃 유저를 정의한다.", "2025-02-28",
                List.of("Sam Chen"), List.of(),
                "경쟁 타이틀 분석을 통해 2D 액션 RPG로 확정됐습니다.", null, List.of(), "leaf", 60, 396, 128, 58, new ArrayList<>()));
        addNode(new RoadmapDto.RoadmapNodeDto("l2", "유니티", "T-021", "active", 70,
                "Unity 2022 LTS 기반 물리 환경을 세팅하고 검증한다.", "2025-04-01",
                List.of("Morgan Lee"), List.of("게임 디자인"),
                "Rigidbody2D 이동과 Physics2D 충돌 레이어 구성이 대부분 완료됐습니다.",
                "경사면 이동 시 캐릭터가 튀는 물리 버그가 재현되고 있습니다.", List.of(), "leaf", 208, 396, 128, 58, new ArrayList<>()));
        addNode(new RoadmapDto.RoadmapNodeDto("l3", "자체 개발", "T-022", "todo", 25,
                "외부 엔진 없이 AABB 충돌·중력 물리 레이어를 구현한다.", "2025-06-30",
                List.of("Sam Chen", "Morgan Lee"), List.of("게임 디자인"),
                "개념 증명 단계로 기본 충돌·중력만 동작합니다.", null, List.of(), "leaf", 356, 396, 128, 58, new ArrayList<>()));
        addNode(new RoadmapDto.RoadmapNodeDto("l4", "검사", "T-031", "todo", 30,
                "강공격·막기·카운터로 구성된 근접 직업을 구현한다.", "2025-07-01",
                List.of("Alex Rivera"), List.of("물리엔진"),
                "카운터 판정 프레임이 넓다는 리뷰가 있어 밸런스 기준값 확정이 선행되어야 합니다.", null, List.of(), "leaf", 504, 396, 128, 58, new ArrayList<>()));
        addNode(new RoadmapDto.RoadmapNodeDto("l5", "궁수", "T-032", "todo", 10,
                "투사체 물리를 기반으로 원거리 직업을 구현한다.", "2025-07-15",
                List.of("Alex Rivera"), List.of("물리엔진"),
                "화살 궤도와 충돌 처리가 물리 레이어에 직접 의존합니다.", null, List.of(), "leaf", 652, 396, 128, 58, new ArrayList<>()));
        addNode(new RoadmapDto.RoadmapNodeDto("l6", "마법사", "T-033", "todo", 0,
                "광역 마법 3종과 VFX 연동을 구현한다.", "2025-08-01",
                List.of("Jordan Park"), List.of("물리엔진"),
                "최저 방어력·최고 딜 포텐셜 구조로, 세 직업 중 밸런스 민감도가 가장 높습니다.", null, List.of(), "leaf", 800, 396, 128, 58, new ArrayList<>()));
        addNode(new RoadmapDto.RoadmapNodeDto("l7", "오크", "T-041", "todo", 0,
                "1챕터 보스의 돌진·지진 패턴과 분노 페이즈를 구현한다.", "2025-08-31",
                List.of("Jordan Park"), List.of("스킬 코딩"),
                "패턴 상태머신이 단순해 보스 AI 구조의 레퍼런스 역할을 합니다.", null, List.of(), "leaf", 948, 396, 128, 58, new ArrayList<>()));
        addNode(new RoadmapDto.RoadmapNodeDto("l8", "트롤", "T-042", "todo", 0,
                "재생 능력과 약점 부위 판정을 갖춘 2챕터 보스를 구현한다.", "2025-09-15",
                List.of("Alex Rivera", "Jordan Park"), List.of("스킬 코딩", "오크"),
                "재생 쿨타임이 짧을 경우 근접 직업의 클리어가 사실상 불가능하다는 테스트 결과가 있습니다.", null, List.of(), "leaf", 1096, 396, 128, 58, new ArrayList<>()));

        edges.add(new RoadmapDto.RoadmapEdgeDto("e-root-m1", "root", "m1"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-root-m2", "root", "m2"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-root-m3", "root", "m3"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-root-m4", "root", "m4"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-m1-l1", "m1", "l1"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-m2-l2", "m2", "l2"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-m2-l3", "m2", "l3"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-m3-l4", "m3", "l4"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-m3-l5", "m3", "l5"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-m3-l6", "m3", "l6"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-m4-l7", "m4", "l7"));
        edges.add(new RoadmapDto.RoadmapEdgeDto("e-m4-l8", "m4", "l8"));
    }

    private void addNode(RoadmapDto.RoadmapNodeDto node) {
        nodes.put(node.getId(), node);
    }

    public RoadmapDto.RoadmapGraphDto getRoadmapGraph() {
        return RoadmapDto.RoadmapGraphDto.builder()
                .nodes(new ArrayList<>(nodes.values()))
                .edges(new ArrayList<>(edges))
                .build();
    }

    public RoadmapDto.RoadmapNodeDto createNode(RoadmapDto.RoadmapNodeDto request) {
        String id = request.getId() == null ? "n" + System.currentTimeMillis() : request.getId();
        RoadmapDto.RoadmapNodeDto node = RoadmapDto.RoadmapNodeDto.builder()
                .id(id)
                .label(request.getLabel() == null ? "새 작업" : request.getLabel())
                .code(request.getCode() == null ? "T-900" : request.getCode())
                .status(request.getStatus() == null ? "todo" : request.getStatus())
                .progress(request.getProgress() == null ? 0 : request.getProgress())
                .goal(request.getGoal() == null ? "" : request.getGoal())
                .dueDate(request.getDueDate() == null ? "2025-12-31" : request.getDueDate())
                .assignees(request.getAssignees() == null ? new ArrayList<>() : request.getAssignees())
                .prerequisites(request.getPrerequisites() == null ? new ArrayList<>() : request.getPrerequisites())
                .aiSummary(request.getAiSummary() == null ? "" : request.getAiSummary())
                .issue(request.getIssue())
                .files(request.getFiles() == null ? new ArrayList<>() : request.getFiles())
                .tier(request.getTier() == null ? "leaf" : request.getTier())
                .x(request.getX() == null ? 60 : request.getX())
                .y(request.getY() == null ? 430 : request.getY())
                .w(request.getW() == null ? 128 : request.getW())
                .h(request.getH() == null ? 58 : request.getH())
                .comments(request.getComments() == null ? new ArrayList<>() : request.getComments())
                .build();
        nodes.put(id, node);
        return node;
    }

    public RoadmapDto.RoadmapNodeDto updateNode(String nodeId, RoadmapDto.RoadmapNodeDto request) {
        RoadmapDto.RoadmapNodeDto current = nodes.get(nodeId);
        if (current == null) {
            throw new IllegalArgumentException("node not found: " + nodeId);
        }
        if (request.getLabel() != null) current.setLabel(request.getLabel());
        if (request.getCode() != null) current.setCode(request.getCode());
        if (request.getStatus() != null) current.setStatus(request.getStatus());
        if (request.getProgress() != null) current.setProgress(request.getProgress());
        if (request.getGoal() != null) current.setGoal(request.getGoal());
        if (request.getDueDate() != null) current.setDueDate(request.getDueDate());
        if (request.getAssignees() != null) current.setAssignees(request.getAssignees());
        if (request.getPrerequisites() != null) current.setPrerequisites(request.getPrerequisites());
        if (request.getAiSummary() != null) current.setAiSummary(request.getAiSummary());
        if (request.getIssue() != null) current.setIssue(request.getIssue());
        if (request.getFiles() != null) current.setFiles(request.getFiles());
        if (request.getComments() != null) current.setComments(request.getComments());
        if (request.getTier() != null) current.setTier(request.getTier());
        if (request.getX() != null) current.setX(request.getX());
        if (request.getY() != null) current.setY(request.getY());
        if (request.getW() != null) current.setW(request.getW());
        if (request.getH() != null) current.setH(request.getH());
        return current;
    }

    public void deleteNode(String nodeId) {
        nodes.remove(nodeId);
        edges.removeIf(edge -> edge.getFrom().equals(nodeId) || edge.getTo().equals(nodeId));
    }

    public RoadmapDto.RoadmapEdgeDto createEdge(RoadmapDto.RoadmapEdgeDto request) {
        String id = request.getId() == null ? "e-" + request.getFrom() + "-" + request.getTo() : request.getId();
        RoadmapDto.RoadmapEdgeDto edge = RoadmapDto.RoadmapEdgeDto.builder()
                .id(id)
                .from(request.getFrom())
                .to(request.getTo())
                .build();
        edges.add(edge);
        return edge;
    }

    public void deleteEdge(String edgeId) {
        edges.removeIf(edge -> edge.getId().equals(edgeId));
    }
}
