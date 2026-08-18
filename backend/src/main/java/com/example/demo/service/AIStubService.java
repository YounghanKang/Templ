package com.example.demo.service;

import com.example.demo.dto.SuggestionDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub", matchIfMissing = true)
public class AIStubService implements AiService {

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText) {
        return generateSuggestions(teamId, specId, specText, null, null);
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId) {
        List<SuggestionDto.Create> out = new ArrayList<>();
        String trimmedFeedback = userFeedback == null ? "" : userFeedback.trim();
        boolean hasFeedback = !trimmedFeedback.isBlank();

        String rawText = (specText == null || specText.isBlank()) ? "프로젝트 기본 기능 개발 명세서" : specText.trim();
        
        // 1. Root task (tempId: node_root)
        String rootLabel = extractRootLabel(rawText);
        String rootTitle = hasFeedback ? "AI 재생성: " + rootLabel : "최종 목표: " + rootLabel;
        String rootGoal = rawText;
        String rootAiSummary = hasFeedback 
                ? "사용자 피드백(" + trimmedFeedback + ")을 반영하여 로드맵 트리가 재구성되었습니다. 각 모듈 간 병목을 사전 점검하세요."
                : "명세서 분석을 통해 프로젝트의 단일 진실 공급원(SSOT) 목표가 수립되었습니다. 역할 분담과 선행 의존성을 확인하세요.";

        String rootJson = String.format(
                "{\"tempId\": \"node_root\", \"parentTempId\": null, \"label\": \"%s\", \"tier\": \"root\", \"code\": \"T-001\", \"goal\": \"%s\", \"dueDate\": \"2026-09-30\", \"assignees\": [\"All Members\"], \"aiSummary\": \"%s\"}",
                escapeJson(rootLabel), escapeJson(truncateOneLine(rootGoal, 120)), escapeJson(rootAiSummary)
        );

        out.add(SuggestionDto.Create.builder()
                .targetType("roadmapNode")
                .targetId(null)
                .title(rootTitle)
                .body(rootGoal)
                .sourceTool("ai-stub")
                .sourceId(String.valueOf(specId))
                .changeJson(rootJson)
                .build());

        // 2. Derive modules based on content or standard development tracks
        List<ModuleTemplate> modules = getModuleTemplates(rawText, hasFeedback, trimmedFeedback);
        int leafGlobalIdx = 10;

        for (int m = 0; m < modules.size(); m++) {
            ModuleTemplate mod = modules.get(m);
            String midTempId = "node_mid_" + (m + 1);
            String midCode = String.format("M-%02d", m + 1);

            String midJson = String.format(
                    "{\"tempId\": \"%s\", \"parentTempId\": \"node_root\", \"label\": \"%s\", \"tier\": \"mid\", \"code\": \"%s\", \"goal\": \"%s\", \"dueDate\": \"%s\", \"assignees\": %s, \"aiSummary\": \"%s\"}",
                    midTempId, escapeJson(mod.label), midCode, escapeJson(mod.goal), mod.dueDate, toJsonArray(mod.assignees), escapeJson(mod.aiSummary)
            );

            out.add(SuggestionDto.Create.builder()
                    .targetType("roadmapNode")
                    .targetId(null)
                    .title("중분류: " + mod.label)
                    .body(mod.goal + "\n\n담당: " + String.join(", ", mod.assignees))
                    .sourceTool("ai-stub")
                    .sourceId(String.valueOf(specId))
                    .changeJson(midJson)
                    .build());

            // Add leaves for each module
            for (int l = 0; l < mod.leaves.size(); l++) {
                LeafTemplate leaf = mod.leaves.get(l);
                String leafTempId = "node_leaf_" + (m + 1) + "_" + (l + 1);
                String leafCode = String.format("T-%03d", leafGlobalIdx++);

                String leafJson = String.format(
                        "{\"tempId\": \"%s\", \"parentTempId\": \"%s\", \"label\": \"%s\", \"tier\": \"leaf\", \"code\": \"%s\", \"goal\": \"%s\", \"dueDate\": \"%s\", \"assignees\": %s, \"aiSummary\": \"%s\"}",
                        leafTempId, midTempId, escapeJson(leaf.label), leafCode, escapeJson(leaf.goal), leaf.dueDate, toJsonArray(leaf.assignees), escapeJson(leaf.aiSummary)
                );

                out.add(SuggestionDto.Create.builder()
                        .targetType("roadmapNode")
                        .targetId(null)
                        .title("세부 작업: " + leaf.label)
                        .body(leaf.goal + "\n\n마감일: " + leaf.dueDate + " / 담당: " + String.join(", ", leaf.assignees))
                        .sourceTool("ai-stub")
                        .sourceId(String.valueOf(specId))
                        .changeJson(leafJson)
                        .build());
            }
        }

        return out;
    }

    private static class ModuleTemplate {
        String label;
        String goal;
        String dueDate;
        List<String> assignees;
        String aiSummary;
        List<LeafTemplate> leaves = new ArrayList<>();

        ModuleTemplate(String label, String goal, String dueDate, List<String> assignees, String aiSummary) {
            this.label = label;
            this.goal = goal;
            this.dueDate = dueDate;
            this.assignees = assignees;
            this.aiSummary = aiSummary;
        }
    }

    private static class LeafTemplate {
        String label;
        String goal;
        String dueDate;
        List<String> assignees;
        String aiSummary;

        LeafTemplate(String label, String goal, String dueDate, List<String> assignees, String aiSummary) {
            this.label = label;
            this.goal = goal;
            this.dueDate = dueDate;
            this.assignees = assignees;
            this.aiSummary = aiSummary;
        }
    }

    private List<ModuleTemplate> getModuleTemplates(String text, boolean hasFeedback, String feedback) {
        List<ModuleTemplate> list = new ArrayList<>();
        String lower = text.toLowerCase();

        // Module 1: Architecture & API
        ModuleTemplate m1 = new ModuleTemplate(
                "API 및 아키텍처 설계",
                "핵심 백엔드 데이터 모델 및 RESTful API 엔드포인트를 설계하고 구현합니다.",
                "2026-08-30",
                List.of("Backend", "Lead Engineer"),
                "초기 DB 스키마 정합성과 외래키 제약조건이 하위 기능 확장에 미칠 영향을 검토해야 합니다."
        );
        m1.leaves.add(new LeafTemplate("데이터베이스 모델링", "핵심 도메인 엔티티 및 인덱스 설계", "2026-08-25", List.of("Backend"), "N+1 쿼리 방지를 위한 페치 조인 계획 수립 필요"));
        m1.leaves.add(new LeafTemplate("핵심 CRUD API 구현", "클라이언트 연동을 위한 규격화된 REST API 개발", "2026-08-30", List.of("Backend"), "API 스웨거 문서화 및 예외 처리 규격 일원화"));
        list.add(m1);

        // Module 2: UI & User Experience
        ModuleTemplate m2 = new ModuleTemplate(
                "프론트엔드 UI/UX 구축",
                "직관적인 계층적 뷰 및 실시간 상태 동기화 인터페이스를 개발합니다.",
                "2026-09-10",
                List.of("Frontend", "UI Designer"),
                "사용자 동시 작업 시 상태 불일치(Conflict)를 방지하는 낙관적 락 UI 피드백이 권장됩니다."
        );
        m2.leaves.add(new LeafTemplate("인터랙티브 캔버스 뷰", "계층적 노드 렌더링 및 드래그 앤 드롭 인터랙션", "2026-09-05", List.of("Frontend"), "대량 노드 렌더링 시 가상화(Virtualization) 고려"));
        m2.leaves.add(new LeafTemplate("상세 정보 패널 및 편집", "할 일 목표, 담당자, 첨부파일 및 AI 요약 상세 패널", "2026-09-10", List.of("Frontend", "UI Designer"), "실시간 변경 사항 자동 저장 및 유효성 검사"));
        list.add(m2);

        // Module 3: Collaboration Orchestration / Integration
        ModuleTemplate m3 = new ModuleTemplate(
                "협업 툴 연동 및 맥락 분석",
                "Slack/GitHub 이벤트 수집 파이프라인 및 AI 조율 기능을 연동합니다.",
                "2026-09-20",
                List.of("AI Engineer", "DevOps"),
                "불필요한 웹훅 노이즈를 1차 규칙 기반 필터로 걸러내어 토큰 비용을 최소화해야 합니다."
        );
        m3.leaves.add(new LeafTemplate("메시지 필터링 파이프라인", "사소한 변경 필터링 및 중요 맥락 변화 감지", "2026-09-15", List.of("DevOps"), "편집거리 및 키워드 기반 1차 필터링 검증"));
        m3.leaves.add(new LeafTemplate("AI 제안 카드 & 승인 루프", "바운더리 충돌 감지 시 Human-in-the-loop 승인 카드 생성", "2026-09-20", List.of("AI Engineer"), "담당자 승인 전에는 실제 변경사항이 격리되도록 보장"));
        list.add(m3);

        if (hasFeedback) {
            ModuleTemplate mFeedback = new ModuleTemplate(
                    "피드백 반영 및 고도화",
                    "사용자 요구사항(" + truncateOneLine(feedback, 40) + ")을 반영한 품질 개선 및 테스트",
                    "2026-09-25",
                    List.of("PM", "QA"),
                    "추가 피드백 사항에 대한 통합 회귀 테스트 및 성능 최적화 진행"
            );
            mFeedback.leaves.add(new LeafTemplate("요구사항 검증", "피드백 항목별 시나리오 테스트", "2026-09-23", List.of("QA"), "예외 케이스 검증"));
            mFeedback.leaves.add(new LeafTemplate("배포 및 릴리즈 점검", "프로덕션 배포 파이프라인 및 모니터링", "2026-09-25", List.of("DevOps"), "서버 헬스체크 및 롤백 전략 수립"));
            list.add(mFeedback);
        }

        return list;
    }

    private String extractRootLabel(String specText) {
        if (specText == null || specText.isBlank()) return "핵심 프로젝트 목표";
        String firstLine = specText.split("\n")[0].replaceAll("[#*`>]", "").trim();
        if (firstLine.isBlank()) firstLine = specText;
        return truncateOneLine(firstLine, 35);
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private String truncateOneLine(String s, int max) {
        if (s == null) return "";
        String one = s.replaceAll("\\s+", " ").trim();
        if (one.length() <= max) return one;
        return one.substring(0, max - 3) + "...";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
