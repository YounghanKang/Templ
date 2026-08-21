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
        return generateSuggestions(teamId, specId, specText, null, null, null);
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId) {
        return generateSuggestions(teamId, specId, specText, userFeedback, baseSuggestionId, null);
    }

    @Override
    public List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId, String existingNodesContext) {
        List<SuggestionDto.Create> out = new ArrayList<>();
        String trimmedFeedback = userFeedback == null ? "" : userFeedback.trim();
        boolean hasFeedback = !trimmedFeedback.isBlank();

        String rawText = (specText == null || specText.isBlank()) ? "프로젝트 기본 목표 수행 명세서" : specText.trim();
        rawText = rawText.replaceAll("(?i)\\[(피드백|feedback)[^\\]]*\\]:?.*", "").trim();
        if (rawText.isBlank()) rawText = "프로젝트 핵심 목표 수행";

        // 1. Root task (tempId: node_root)
        String rootLabel = extractRootLabel(rawText, hasFeedback, trimmedFeedback);
        String rootTitle = hasFeedback ? "AI 재생성: " + rootLabel : "최종 목표: " + rootLabel;
        String defaultDueDate = java.time.LocalDate.now().plusMonths(1).toString();
        String rootGoal = hasFeedback ? rawText + "\n\n[피드백 반영]: " + trimmedFeedback : rawText;
        String rootAiSummary = hasFeedback
                ? "사용자 피드백(" + trimmedFeedback + ")을 반영하여 로드맵 트리가 체계적으로 재정립되었습니다. 단계별 선행 조건과 리스크를 확인하세요."
                : "명세서 분석을 통해 프로젝트의 핵심 목표와 실행 체계가 수립되었습니다. 역할 분담과 일정 계획을 점검하세요.";

        String rootJson = String.format(
                "{\"tempId\": \"node_root\", \"parentTempId\": null, \"label\": \"%s\", \"tier\": \"root\", \"code\": \"T-001\", \"goal\": \"%s\", \"dueDate\": \"%s\", \"assignees\": [\"All Members\"], \"aiSummary\": \"%s\"}",
                escapeJson(rootLabel), escapeJson(truncateOneLine(rootGoal, 120)), defaultDueDate, escapeJson(rootAiSummary)
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

        // 2. Derive modules based on domain classification, PRD markdown structure, and smart feedback integration
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
        String fbLower = hasFeedback ? feedback.toLowerCase() : "";

        // Check explicit software / IT / platform request
        boolean isExplicitSoftware = lower.contains("웹") || lower.contains("앱") || lower.contains("어플")
                || lower.contains("서버") || lower.contains("api") || lower.contains("시스템")
                || lower.contains("소프트웨어") || lower.contains("코딩") || lower.contains("백엔드") || lower.contains("프론트엔드")
                || lower.contains("frontend") || lower.contains("backend") || lower.contains("플랫폼") || lower.contains("platform")
                || lower.contains("github") || lower.contains("git") || lower.contains("db") || lower.contains("database")
                || lower.contains("아카이빙") || lower.contains("erp") || lower.contains("블로그") || lower.contains("개발")
                || lower.contains("동아리") || lower.contains("학회") || lower.contains("리크루팅")
                || (lower.contains("만들기") && (lower.contains("사이트") || lower.contains("서비스") || lower.contains("홈페이지")));

        // Check user feedback removal intents
        boolean excludeReviews = fbLower.contains("리뷰") && (fbLower.contains("빼") || fbLower.contains("제외") || fbLower.contains("삭제") || fbLower.contains("없애") || fbLower.contains("말아") || fbLower.contains("필요 없"));
        boolean hasCableOrAccessory = fbLower.contains("선") || fbLower.contains("케이블") || fbLower.contains("부속") 
                || fbLower.contains("악세사리") || fbLower.contains("액세서리") || fbLower.contains("어댑터") || fbLower.contains("젠더");

        if (!isExplicitSoftware) {
            // =========================================================================
            // 1-A. COMPUTER HARDWARE / PARTS PURCHASING DOMAIN (SSD, RAM, CPU, GPU, etc.)
            // =========================================================================
            boolean isPcHardware = lower.contains("컴퓨터") || lower.contains("부품") || lower.contains("cpu") 
                    || lower.contains("ram") || lower.contains("ssd") || lower.contains("그래픽") 
                    || lower.contains("메인보드") || lower.contains("본체") || lower.contains("하드웨어") || lower.contains("pc");

            if (isPcHardware) {
                // Mid 1: 요구 사양 및 부품 호환성/예산 정의
                ModuleTemplate m1 = new ModuleTemplate(
                        "목표 사양 및 부품 간 호환성/예산 정의",
                        "용도(게이밍/작업/사무용)에 따른 요구 성능을 정의하고 부품 간 규격 호환성 및 예산 한도를 확정합니다.",
                        "2026-08-25",
                        List.of("기획/구매 담당"),
                        "CPU 소켓 규격, 메인보드 칩셋, RAM DDR 세대 및 SSD M.2 슬롯 규격을 사전에 교차 검증해야 조립 불가 문제를 방지할 수 있습니다."
                );
                m1.leaves.add(new LeafTemplate("용도별 목표 성능 및 총 예산 상한선 확정", "주요 사용 목적에 맞춘 부품별(CPU/RAM/SSD) 적정 예산 배분", "2026-08-20", List.of("구매 담당"), "가성비 및 예산 초과 방지"));
                m1.leaves.add(new LeafTemplate("메인보드/파워 소켓 및 인터페이스 규격 검증", "CPU 소켓 일치 여부, PCIe 버전 및 정격 파워 용량 사전 실측", "2026-08-25", List.of("하드웨어 담당"), "부품 간 규격 불일치 방지"));
                list.add(m1);

                // Mid 2: 부품별 스펙 및 벤치마크 비교 분석
                String m2Title = excludeReviews ? "부품별 기술 스펙 및 성능 벤치마크 분석" : "부품별 스펙 및 벤치마크 비교 분석";
                ModuleTemplate m2 = new ModuleTemplate(
                        m2Title,
                        "예산 범위 내 후보 부품군(CPU, RAM, SSD)의 클럭, 대역폭 및 성능 벤치마크 데이터를 다각도로 비교합니다.",
                        "2026-09-05",
                        List.of("조사/리서치 담당"),
                        "단순 가격뿐 아니라 실측 벤치마크 점수, 전력 소모량(TDP), 발열 제어 및 제조사 무상 A/S 보증 기간을 종합 평가해야 합니다."
                );
                m2.leaves.add(new LeafTemplate("CPU 코어/클럭 및 벤치마크 지표 비교", "싱글/멀티코어 벤치마크 점수 및 내장 그래픽 유무 확인", "2026-08-30", List.of("조사 담당"), "성능 대비 가격 비교표 작성"));
                if (excludeReviews) {
                    m2.leaves.add(new LeafTemplate("RAM 클럭/타이밍 및 SSD 읽기/쓰기 속도 비교", "DDR 대역폭(MHz), CL 타이밍 및 NVMe 순차/랜덤 IOPS 분석", "2026-09-05", List.of("하드웨어 담당"), "데이터 처리 지연 최소화"));
                } else {
                    m2.leaves.add(new LeafTemplate("실구매자 결함 사례 및 제조사 A/S 정책 확인", "고장 빈도, 발열 이슈 및 공식 유통사 보증 기간 검토", "2026-09-05", List.of("조사 담당"), "신뢰성 및 A/S 용이성 검증"));
                }
                list.add(m2);

                // Mid 3: 판매처별 최저가 비교 및 분할/일괄 주문
                ModuleTemplate m3 = new ModuleTemplate(
                        "판매처별 최저가 비교 및 주문 결제",
                        "공식 대리점, 오픈마켓 특가 및 묶음 배송 혜택을 비교하여 가장 경제적이고 안전한 경로로 결제합니다.",
                        "2026-09-15",
                        List.of("구매/결제 담당"),
                        "병행수입/벌크 제품 여부를 확인하고 국내 정품 보증 스티커 동봉 여부를 사전에 체크해야 합니다."
                );
                m3.leaves.add(new LeafTemplate("부품별 판매처 최저가 및 쿠폰/카드 할인 비교", "오픈마켓 및 전문몰 견적 비교를 통한 최종 실구매가 산출", "2026-09-10", List.of("구매 담당"), "최저가 조합 구성"));
                m3.leaves.add(new LeafTemplate("정품 확인 및 주문 완료/배송 추적", "국내 정품 인증 제품 주문 및 송장 번호 실시간 추적", "2026-09-15", List.of("구매 담당"), "영수증 및 주문 내역서 보관"));
                list.add(m3);

                // Mid 4: 배송 수령, 부품 장착/조립 및 동작 테스트
                ModuleTemplate m4 = new ModuleTemplate(
                        "배송 수령, 부품 장착/조립 및 성능 검수",
                        "도착한 부품의 정품 씰을 확인하고, 메인보드에 결착하여 바이오스(BIOS) 인식 및 정상 동작을 검증합니다.",
                        "2026-09-25",
                        List.of("검수/조립 담당"),
                        "정전기 방지 조치를 취하고, CPU 핀 휨 여부 확인 및 최초 부팅 시 바이오스 펌웨어 인식을 반드시 확인해야 합니다."
                );
                m4.leaves.add(new LeafTemplate("개봉 검수 및 외관/정품 씰 훼손 점검", "박스 훼손, 밀봉 씰 부착 상태 및 CPU 핀/골드핑거 외관 검수", "2026-09-20", List.of("검수 담당"), "언박싱 하자 유무 확인"));
                m4.leaves.add(new LeafTemplate("부품 장착 및 바이오스(BIOS) 부팅 테스트", "CPU/RAM/SSD 결착 후 메모리 XMP/EXPO 적용 및 부하 테스트", "2026-09-25", List.of("조립 담당"), "정상 인식 및 벤치마크 통과 확인"));
                list.add(m4);

                return list;
            }

            // =========================================================================
            // 1-B. GENERAL PURCHASING & PROCUREMENT DOMAIN (e.g., 스피커 구매하기, 장비 구매)
            // =========================================================================
            if (lower.contains("구매") || lower.contains("구입") || lower.contains("사기") || lower.contains("장만") 
                    || lower.contains("쇼핑") || lower.contains("주문") || lower.contains("스피커") || lower.contains("buy")) {
                
                String m1Label = hasCableOrAccessory ? "요구 사양 및 부속 케이블 예산 배분" : "요구 사양 및 총 예산 설정";
                ModuleTemplate m1 = new ModuleTemplate(
                        m1Label,
                        "사용 목적, 설치 환경(공간/단자 호환성) 및 총 예산 한도를 명확히 정의하고 배분합니다.",
                        "2026-08-25",
                        List.of("기획/구매 담당"),
                        "사용 환경에 적합한 규격과 단자 호환성을 사전 검토해야 불필요한 추가 지출 및 반품을 방지할 수 있습니다."
                );
                m1.leaves.add(new LeafTemplate("사용 목적 및 설치 환경/공간 실측", "배치 공간 크기, 사용 목적 및 연결 기기 단자 규격 확인", "2026-08-20", List.of("구매 담당"), "설치 위치 및 연결 인터페이스 점검"));
                m1.leaves.add(new LeafTemplate(hasCableOrAccessory ? "본체 및 호환 케이블/부속품 예산 배분" : "가격대별 예산 상한선 확정", "가용 예산 범위 내 본체 및 필수 악세사리/케이블 비용 분배", "2026-08-25", List.of("예산 담당"), "가성비 및 예산 초과 방지"));
                list.add(m1);

                String m2Label = hasCableOrAccessory ? "후보 모델 및 호환 케이블 비교 분석" : (excludeReviews ? "후보 모델 스펙 및 성능 비교 분석" : "후보 모델 스펙 및 리뷰 비교");
                ModuleTemplate m2 = new ModuleTemplate(
                        m2Label,
                        "예산 범위 내 적합한 제품군을 선별하고 스펙, 내구성 및 호환성을 다각도로 비교 분석합니다.",
                        "2026-09-05",
                        List.of("조사/리서치 담당"),
                        "단순 가격 비교뿐 아니라 주요 기능, A/S 정책 및 부속품 호환성을 종합적으로 고려해야 합니다."
                );
                m2.leaves.add(new LeafTemplate("주요 브랜드별 인기 모델 스펙 비교", "성능, 내구성, 주요 기능 및 출력/성능 지표 분석", "2026-08-30", List.of("조사 담당"), "스펙 비교표 작성"));
                if (hasCableOrAccessory) {
                    m2.leaves.add(new LeafTemplate("호환 케이블/부속품 규격 및 품질 검토", "단자 매칭(AUX/광케이블/RCA), 케이블 길이 및 차폐 성능 검토", "2026-09-05", List.of("장비 담당"), "단자 접촉 불량 및 손실 최소화"));
                } else if (excludeReviews) {
                    m2.leaves.add(new LeafTemplate("제조사 보증 기간 및 기술 사양 검증", "A/S 지원 정책, 소비 전력 및 상세 기술 규격 검토", "2026-09-05", List.of("조사 담당"), "기술 사양 충족 여부 확인"));
                } else {
                    m2.leaves.add(new LeafTemplate("실구매자 리뷰 및 A/S 조건 확인", "장단점 분석, 고장 빈도 및 보증 기간 검토", "2026-09-05", List.of("조사 담당"), "실사용 만족도 검증"));
                }
                list.add(m2);

                String m3Label = hasCableOrAccessory ? "최적 판매처 선정 및 케이블 묶음 결제" : "최적 판매처 선정 및 주문 결제";
                ModuleTemplate m3 = new ModuleTemplate(
                        m3Label,
                        "공식 판매처, 오픈마켓 및 프로모션 할인을 비교하여 안전하고 가장 유리한 조건으로 구매합니다.",
                        "2026-09-15",
                        List.of("구매/결제 담당"),
                        "정품 보증 여부, 배송 소요 시간 및 무료 반품 조건을 사전에 확인해야 합니다."
                );
                m3.leaves.add(new LeafTemplate("판매처별 할인 혜택 및 최저가 비교", "카드 할인, 쿠폰, 사은품 및 무료 배송 여부 비교", "2026-09-10", List.of("구매 담당"), "최종 실구매가 산출"));
                m3.leaves.add(new LeafTemplate(hasCableOrAccessory ? "본체 및 케이블 묶음 주문 결제" : "주문 완료 및 배송 추적", "안전 결제 진행 및 배송 송장 번호 확인", "2026-09-15", List.of("구매 담당"), "결제 영수증 및 주문 확인서 보관"));
                list.add(m3);

                String m4Label = hasCableOrAccessory ? "수령 후 케이블 결선 및 작동 검수" : "수령 후 연결 설치 및 작동 검수";
                ModuleTemplate m4 = new ModuleTemplate(
                        m4Label,
                        "도착한 제품을 개봉하여 외관 하자를 점검하고, 정상 작동 및 최적 성능을 테스트합니다.",
                        "2026-09-25",
                        List.of("검수/사용 담당"),
                        "초기 불량 발생 시 즉시 교환/환불을 진행할 수 있도록 언박싱 및 테스트 과정을 기록합니다."
                );
                m4.leaves.add(new LeafTemplate("외관 검수 및 구성품/케이블 누락 확인", "박스 훼손, 스크래치, 기본 부속품 및 추가 주문 케이블 확인", "2026-09-20", List.of("검수 담당"), "언박싱 상태 확인"));
                m4.leaves.add(new LeafTemplate(hasCableOrAccessory ? "케이블 결선 및 출력/신호 검증" : "초기 전원 연결 및 기능 테스트", "설치 완료 후 각 모드별 출력 및 노이즈 유무 검증", "2026-09-25", List.of("사용 담당"), "초기 불량 여부 최종 확인"));
                list.add(m4);

                return list;
            }

            // =========================================================================
            // 2. TRIP, EVENT & WORKSHOP DOMAIN (Strictly non-software)
            // =========================================================================
            if (lower.contains("여행") || lower.contains("휴가") || lower.contains("캠핑") 
                    || lower.contains("파티") || lower.contains("워크샵") || lower.contains("축제")) {
                ModuleTemplate m1 = new ModuleTemplate(
                        "일정 및 목적지/장소 확정",
                        "전체 일정, 예산 한도 및 핵심 목적지/장소를 확정합니다.",
                        "2026-08-25",
                        List.of("기획 담당"),
                        "참여자 선호도와 이동 동선의 효율성을 사전에 고려해야 합니다."
                );
                m1.leaves.add(new LeafTemplate("참여자 인원 및 일정 조율", "일정 투표 및 참석자 명단 확정", "2026-08-20", List.of("기획 담당"), "일정 픽스"));
                m1.leaves.add(new LeafTemplate("목적지 및 주요 거점 선정", "방문 장소 및 이동 동선 초안 작성", "2026-08-25", List.of("기획 담당"), "동선 최적화"));
                list.add(m1);

                ModuleTemplate m2 = new ModuleTemplate(
                        "교통편 및 숙소/공간 예약",
                        "최적 이동 경로의 교통수단과 편안한 숙소/대관 장소를 신속히 예약합니다.",
                        "2026-09-05",
                        List.of("예약 담당"),
                        "성수기 매진 리스크를 대비하여 조기 예약 및 취소 환불 규정을 검토합니다."
                );
                m2.leaves.add(new LeafTemplate("교통편(항공/열차/차량) 예매", "탑승 시간 및 좌석 배정", "2026-08-30", List.of("예약 담당"), "예약 확인서 보관"));
                m2.leaves.add(new LeafTemplate("숙소 및 주요 프로그램 공간 예약", "체크인 규정 확인 및 인원별 객실 배정", "2026-09-05", List.of("예약 담당"), "위치 접근성 점검"));
                list.add(m2);

                ModuleTemplate m3 = new ModuleTemplate(
                        "세부 프로그램 및 준비물 체크리스트",
                        "시간대별 활동 계획 수립, 식사 장소 선정 및 필수 준비물을 점검합니다.",
                        "2026-09-15",
                        List.of("운영 담당"),
                        "우천 등 기상 변화나 돌발 상황에 대응할 수 있는 대체 플랜(Plan B)을 마련합니다."
                );
                m3.leaves.add(new LeafTemplate("일자별 세부 타임테이블 구성", "식사, 레크리에이션 및 자유 시간 배분", "2026-09-10", List.of("기획 담당"), "여유 시간 확보"));
                m3.leaves.add(new LeafTemplate("개인/공용 준비물 리스트 배포", "필수 지참 물품 확인 및 역할별 챙길 물품 점검", "2026-09-15", List.of("운영 담당"), "누락 물품 방지"));
                list.add(m3);

                ModuleTemplate m4 = new ModuleTemplate(
                        "현장 실행 및 사후 정산",
                        "일정을 원활히 진행하고, 발생 비용을 정산하며 사진과 후기를 공유합니다.",
                        "2026-09-25",
                        List.of("총무/운영 담당"),
                        "공용 지출 영수증을 누락 없이 수합하여 투명하게 정산합니다."
                );
                m4.leaves.add(new LeafTemplate("현장 진행 및 일정 관리", "타임라인 준수 및 안전 사고 예방", "2026-09-20", List.of("운영 담당"), "비상 연락망 유지"));
                m4.leaves.add(new LeafTemplate("경비 정산 및 후기 공유", "영수증 정산, N분의 1 송금 및 단체 사진 아카이빙", "2026-09-25", List.of("총무 담당"), "정산 내역 공개"));
                list.add(m4);

                return list;
            }

            // =========================================================================
            // 3. CRAFTING, MAKING & GENERAL REAL-WORLD TASK DOMAIN (e.g., 종이학 접기, 요리, DIY)
            // =========================================================================
            boolean hasPractice = fbLower.contains("연습") || fbLower.contains("모의") || fbLower.contains("실습") || fbLower.contains("샘플");

            ModuleTemplate m1 = new ModuleTemplate(
                    "준비물 및 작업 환경 조성",
                    "목표 과업에 필요한 기본 도구, 재료 및 작업 환경을 사전에 완비합니다.",
                    "2026-08-25",
                    List.of("준비 담당"),
                    "품질 좋은 재료와 적절한 작업 공간을 갖추어야 결과물의 완성도가 높아집니다."
            );
            m1.leaves.add(new LeafTemplate("필수 재료 및 도구 구비", "규격에 맞는 재료 및 보조 도구 준비", "2026-08-20", List.of("준비 담당"), "재료 품질 확인"));
            m1.leaves.add(new LeafTemplate("작업 순서 및 매뉴얼 확인", "기본 작업 절차 숙지 및 주의사항 점검", "2026-08-25", List.of("준비 담당"), "가이드라인 확인"));
            list.add(m1);

            if (hasPractice) {
                ModuleTemplate mPractice = new ModuleTemplate(
                        "사전 연습 및 예비 실습",
                        "본 작업 전 모의 실습을 통해 손에 익히고 발생 가능한 오차를 사전에 파악합니다.",
                        "2026-09-02",
                        List.of("실행 담당"),
                        "연습 과정을 통해 디테일한 노하우를 습득하고 본 작업의 실패율을 낮춥니다."
                );
                mPractice.leaves.add(new LeafTemplate("연습용 재료를 통한 모의 실습", "부담 없는 대체 재료로 전 과정을 1회 이상 실습", "2026-08-28", List.of("실행 담당"), "핵심 기법 숙달"));
                mPractice.leaves.add(new LeafTemplate("연습 결과 분석 및 보완점 도출", "실습 중 발생한 오차 분석 및 본 작업 시 적용 팁 정리", "2026-09-02", List.of("검수 담당"), "완성도 기준 확립"));
                list.add(mPractice);
            }

            ModuleTemplate m2 = new ModuleTemplate(
                    "본 작업 진행 및 세부 공정",
                    "순서에 따라 정밀하게 공정을 진행하고 단계별 완성도를 높입니다.",
                    "2026-09-15",
                    List.of("실행 담당"),
                    "각 단계마다 정렬과 마감을 꼼꼼히 점검하여 뒤틀림을 방지해야 합니다."
            );
            m2.leaves.add(new LeafTemplate("기초 형태 형성 및 주요 공정 수행", "정확한 규격에 맞춘 기본 구조 제작", "2026-09-08", List.of("실행 담당"), "기준선 및 균형 유지"));
            m2.leaves.add(new LeafTemplate("세부 디테일 가공 및 결합", "주요 파트 정밀 가공 및 형태 다듬기", "2026-09-15", List.of("실행 담당"), "마감 품질 확인"));
            list.add(m2);

            ModuleTemplate m3 = new ModuleTemplate(
                    "최종 검수 및 완성물 보관/활용",
                    "완성된 결과물의 상태를 점검하고, 마감 손질 후 안전하게 보관하거나 공유합니다.",
                    "2026-09-25",
                    List.of("검수 담당"),
                    "최종 품질 체크리스트를 점검하여 불완전한 요소를 보완합니다."
            );
            m3.leaves.add(new LeafTemplate("형태 및 품질 최종 검수", "균형, 마감 상태 및 외관 결함 전수 점검", "2026-09-20", List.of("검수 담당"), "체크리스트 점검"));
            m3.leaves.add(new LeafTemplate("최종 마감 및 보관/전달", "보호 포장 또는 지정 위치 배치 및 정리", "2026-09-25", List.of("운영 담당"), "아카이빙 완료"));
            list.add(m3);

            return list;
        }

        // =========================================================================
        // 4. CLUB / COMMUNITY MANAGEMENT & ARCHIVING DOMAIN (e.g., CLUB-IN)
        // =========================================================================
        if (lower.contains("동아리") || lower.contains("학회") || lower.contains("클럽") || lower.contains("club")
                || lower.contains("리크루팅") || lower.contains("아카이빙") || lower.contains("출결") || lower.contains("회계")) {
            
            ModuleTemplate m1 = new ModuleTemplate(
                    "대외 홍보 및 리크루팅 시스템",
                    "랜딩 페이지, 기수별 프로젝트 쇼케이스 및 지원서 접수/평가 파이프라인을 구축합니다.",
                    "2026-08-30",
                    List.of("Frontend", "Design"),
                    "기수별 커스텀 폼 빌더, 포트폴리오 첨부 및 지원 결과 개별 조회 기능을 구현해야 합니다."
            );
            m1.leaves.add(new LeafTemplate("랜딩 페이지 및 프로젝트 쇼케이스", "동아리 소개, 연혁 및 기수별 프로젝트 카드뷰 개발", "2026-08-25", List.of("Frontend"), "GitHub 및 데모 링크 연동"));
            m1.leaves.add(new LeafTemplate("지원서 폼 빌더 및 지원 결과 조회", "문항 커스텀 폼, 임시저장 및 이메일/전화번호 결과 조회", "2026-08-30", List.of("Fullstack"), "제출 후 수정 방지 로직"));
            list.add(m1);

            ModuleTemplate m2 = new ModuleTemplate(
                    "회원 및 권한 체계 관리",
                    "기수별/파트별 부원 DB 구축 및 상태(활동/수료/휴학)와 역할 권한(Admin/정회원/비회원)을 관리합니다.",
                    "2026-09-08",
                    List.of("Backend", "Security"),
                    "역할 기반 접근 제어(RBAC) 및 기수 교체 시 운영진 권한 인수인계 체계를 설계해야 합니다."
            );
            m2.leaves.add(new LeafTemplate("기수/파트별 부원 DB 및 프로필 관리", "부원 명단, 파트(Frontend/Backend/Design 등) 및 상태 관리", "2026-09-05", List.of("Backend"), "부원 상태 트랜잭션"));
            m2.leaves.add(new LeafTemplate("역할 기반 접근 제어(RBAC) 및 인증", "Admin/정회원/비회원 권한 격리 및 세션/토큰 인증", "2026-09-08", List.of("Backend"), "인가 인터셉터 적용"));
            list.add(m2);

            ModuleTemplate m3 = new ModuleTemplate(
                    "실시간 출결 및 행사 관리",
                    "정기 세션/행사 시 GPS 위치 기반 또는 3분 갱신 OTP/QR 코드를 통한 실시간 출석 체크를 구현합니다.",
                    "2026-09-18",
                    List.of("Mobile/Web", "Backend"),
                    "지각/결석 누적에 따른 경고 자동 카운팅 및 행사 일정 캘린더를 연동해야 합니다."
            );
            m3.leaves.add(new LeafTemplate("GPS/OTP/QR 실시간 출석 체크", "3분 갱신 OTP/QR 발급 및 위치 기반 출결 검증", "2026-09-12", List.of("Fullstack"), "출결 부정행위 방지"));
            m3.leaves.add(new LeafTemplate("출결 통계 및 경고 자동 카운팅", "지각/결석 누적 집계, 패널티 자동 부과 및 알림", "2026-09-18", List.of("Backend"), "자동 카운팅 트리거"));
            list.add(m3);

            ModuleTemplate m4 = new ModuleTemplate(
                    "동아리 회계 및 장부 관리 (ERP)",
                    "회비 수입/지출 내역을 투명하게 공개하고, 항목별 영수증 증빙 첨부 및 실시간 잔액을 계산합니다.",
                    "2026-09-25",
                    List.of("Fullstack", "DBA"),
                    "영수증 이미지 첨부 및 예산 대비 잔액 계산 트랜잭션의 정합성을 보장해야 합니다."
            );
            m4.leaves.add(new LeafTemplate("회비 수입/지출 장부 및 실시간 잔액 계산", "회비 납부 현황, 지출 내역 기록 및 예산 잔액 집계", "2026-09-20", List.of("Backend"), "금액 정합성 검증"));
            m4.leaves.add(new LeafTemplate("지출 영수증 증빙 첨부 및 투명 공개 UI", "영수증 이미지 업로드, 승인 내역 및 공개 장부 뷰", "2026-09-25", List.of("Frontend"), "S3 이미지 업로드 연동"));
            list.add(m4);

            ModuleTemplate m5 = new ModuleTemplate(
                    "학술 스터디 및 기술 블로그 아카이빙",
                    "스터디 개설/승인/과제 제출 파이프라인과 Markdown 기반 동아리 기술 블로그를 개발합니다.",
                    "2026-09-30",
                    List.of("Frontend", "Backend"),
                    "Markdown 에디터(코드 하이라이팅) 및 태그/기수별 아카이빙 검색을 지원해야 합니다."
            );
            m5.leaves.add(new LeafTemplate("스터디 개설/모집 및 주차별 과제 제출", "스터디 승인 플로우, 부원 모집 및 과제 제출/피드백", "2026-09-28", List.of("Fullstack"), "주차별 과제 트래킹"));
            m5.leaves.add(new LeafTemplate("Markdown 기술 블로그 및 태그 검색", "코드 하이라이팅 에디터, 태그/기수별 필터링 아카이빙", "2026-09-30", List.of("Frontend"), "기술 자산 아카이빙"));
            list.add(m5);

            return list;
        }

        // =========================================================================
        // 4. COLLABORATION ORCHESTRATOR / DIRECTORY / PIPELINE DOMAIN
        // =========================================================================
        boolean isOrchestrator = lower.contains("오케스트레이터") || lower.contains("디렉토리") || lower.contains("가상 디렉토리")
                || (lower.contains("slack") && lower.contains("github")) || lower.contains("맥락 분석") || lower.contains("충돌 감지")
                || lower.contains("human-in-the-loop");

        if (isOrchestrator) {
            ModuleTemplate m1 = new ModuleTemplate(
                    "명세서 기반 디렉토리 자동 생성",
                    "자연어 프로젝트 명세서를 분석하여 계층적 가상 디렉토리(WBS 트리) 구조를 자동으로 분해 및 형성합니다.",
                    "2026-08-30",
                    List.of("AI/Backend", "PM"),
                    "자연어 요구사항을 실현 가능한 작업 단위로 계층화하고, 프롬프트 기반 재생성 및 직접 편집 기능을 제공해야 합니다."
            );
            m1.leaves.add(new LeafTemplate("자연어 명세서 파싱 및 WBS 분해 엔진", "LLM 기반 자연어 분석 및 3계층 디렉토리 트리 구조화", "2026-08-25", List.of("AI/Backend"), "모듈 및 세부 작업 계층 매핑 검증"));
            m1.leaves.add(new LeafTemplate("디렉토리 트리 프리뷰 및 피드백 재생성 UI", "미리보기 그래프 렌더링 및 사용자 수정 프롬프트 처리", "2026-08-30", List.of("Frontend"), "직관적인 계층형 노드 시각화"));
            list.add(m1);

            ModuleTemplate m2 = new ModuleTemplate(
                    "할 일(Task) 노드 상세 및 메타데이터 관리",
                    "각 디렉토리/할 일 노드의 목표, 마감일, 담당자, 선행 의존 작업 및 AI 요약 정보를 관리합니다.",
                    "2026-09-10",
                    List.of("Fullstack"),
                    "노드별 결과물 업로드 공간과 진행 상태(Todo/InProgress/Done) 트래킹을 연동해야 합니다."
            );
            m2.leaves.add(new LeafTemplate("노드 상세 속성 및 선행 작업 의존성 설정", "목표, 기한, 담당자 R&R 및 그래프 엣지 연결 관리", "2026-09-05", List.of("Backend"), "순환 참조 방지 및 상태 동기화"));
            m2.leaves.add(new LeafTemplate("결과물 업로드 및 AI 진행상황 자동 요약", "산출물 파일/링크 첨부 및 LLM 기반 작업 진행도 요약", "2026-09-10", List.of("AI/Frontend"), "산출물 변경 시 자동 요약 갱신"));
            list.add(m2);

            boolean splitIntegrations = fbLower.contains("분리") || fbLower.contains("webhook") || fbLower.contains("봇");
            if (splitIntegrations) {
                ModuleTemplate mSlack = new ModuleTemplate(
                        "Slack 봇 연동 및 실시간 대화 수집",
                        "Slack Socket Mode/Bot 연동을 통해 팀 채널의 실시간 대화 및 논의 사항을 수집합니다.",
                        "2026-09-15",
                        List.of("Integration", "Backend"),
                        "Slack 권한 토큰 관리 및 채널별 이벤트 필터링이 필요합니다."
                );
                mSlack.leaves.add(new LeafTemplate("Slack Bot Token & Socket Mode 연동", "채널 메시지 수신 및 알림 발송 봇 구성", "2026-09-12", List.of("Integration"), "웹소켓 재연결 및 누락 방지"));
                mSlack.leaves.add(new LeafTemplate("메시지 정제 및 1차 규칙 기반 필터링", "오타/사소한 변경 필터링 및 키워드 매칭", "2026-09-15", List.of("Backend"), "불필요한 LLM 호출 최소화"));
                list.add(mSlack);

                ModuleTemplate mGithub = new ModuleTemplate(
                        "GitHub Webhook 및 이슈/PR 이벤트 수집",
                        "GitHub Webhook을 통해 커밋, PR, 이슈 생성/변경 이벤트를 실시간으로 감지합니다.",
                        "2026-09-18",
                        List.of("DevOps", "Backend"),
                        "Webhook Secret 검증 및 페이로드 변경 diff 추출을 수행합니다."
                );
                mGithub.leaves.add(new LeafTemplate("GitHub Webhook 엔드포인트 및 서명 검증", "HMAC SHA-256 서명 검증 및 이벤트 디스패처", "2026-09-15", List.of("Backend"), "보안 검증 및 비동기 큐잉"));
                mGithub.leaves.add(new LeafTemplate("코드 변경 Diff 및 이슈 내용 추출", "커밋/PR diff 파싱 및 핵심 변경 사항 추출", "2026-09-18", List.of("Backend"), "이벤트 메타데이터 표준화"));
                list.add(mGithub);
            } else {
                ModuleTemplate m3 = new ModuleTemplate(
                        "AI 맥락 분석 및 필터링 파이프라인 (Slack/GitHub)",
                        "Slack 메시지와 GitHub 이벤트를 1차 규칙 기반으로 필터링하고, 중요한 맥락 변화만 LLM에 전달합니다.",
                        "2026-09-18",
                        List.of("Backend", "AI"),
                        "모든 이벤트를 LLM에 보내지 않고 diff/키워드 필터링을 거쳐 토큰 비용을 최적화해야 합니다."
                );
                m3.leaves.add(new LeafTemplate("Slack/GitHub 이벤트 수신 및 1차 규칙 필터링", "편집거리(diff) 및 키워드 기반 노이즈 제거", "2026-09-12", List.of("Backend"), "미미한 변경 1차 차단"));
                m3.leaves.add(new LeafTemplate("LLM 맥락 분석 및 디렉토리 매칭 파이프라인", "이벤트를 특정 디렉토리 노드에 매핑하고 맥락 평가", "2026-09-18", List.of("AI/Backend"), "정확한 노드 스코프 타겟팅"));
                list.add(m3);
            }

            ModuleTemplate m4 = new ModuleTemplate(
                    "바운더리(역할) 침해 감지 및 Human-in-the-loop 조율",
                    "담당자 간 작업 충돌 및 목표 이탈을 감지하여 해당 디렉토리에 한정한 제안 카드를 발행하고 사람의 승인 하에 조율합니다.",
                    "2026-09-25",
                    List.of("AI", "Frontend"),
                    "AI가 독단적으로 변경하지 않고, 담당자 승인 시에만 로드맵에 실제 반영되는 안전장치를 구현해야 합니다."
            );
            m4.leaves.add(new LeafTemplate("역할 충돌 감지 및 디렉토리 한정 경고 생성", "R&R 중복 및 방향성 이탈 시 국소 경고 카드 발행", "2026-09-20", List.of("AI/Backend"), "전체 알림 공해 방지"));
            m4.leaves.add(new LeafTemplate("Human-in-the-loop 승인/반려 인터랙션", "제안 카드 리뷰, 승인 시 자동 조율 및 히스토리 아카이빙", "2026-09-25", List.of("Frontend"), "투명한 승인 트랜잭션"));
            list.add(m4);

            ModuleTemplate m5 = new ModuleTemplate(
                    "부가 기능 및 디렉토리 도식도 시각화",
                    "팀 프로필 공유, 전체 계층 구조 도식도 인터랙션 및 진행 상황 요약 대시보드를 제공합니다.",
                    "2026-09-30",
                    List.of("Frontend", "Design"),
                    "노드 클릭 시 상세 이동 및 완료 항목 시각적 구분을 지원합니다."
            );
            m5.leaves.add(new LeafTemplate("디렉토리 도식도 시각화 및 인터랙션", "캔버스 기반 계층 구조 줌/패닝 및 노드 네비게이션", "2026-09-28", List.of("Frontend"), "매끄러운 인터랙션 구현"));
            m5.leaves.add(new LeafTemplate("팀 프로필 및 외부 협업 링크 공유", "Slack, GitHub, Notion 등 연동 상태 및 프로필 공유", "2026-09-30", List.of("Fullstack"), "통합 프로필 관리"));
            list.add(m5);

            return list;
        }

        // Generic Software Project
        ModuleTemplate m1 = new ModuleTemplate(
                "사용자 인증 및 계정 체계",
                "안전한 계정 인증(JWT), 세션 관리 및 역할 기반 권한 제어(RBAC)를 구축합니다.",
                "2026-08-30",
                List.of("Backend", "Security"),
                "인증 토큰 탈취 방지 및 API 엔드포인트별 인가 인터셉터를 철저히 검증해야 합니다."
        );
        m1.leaves.add(new LeafTemplate("회원가입 및 JWT 인증 API", "비밀번호 단방향 암호화(BCrypt) 및 토큰 발급", "2026-08-25", List.of("Backend"), "입력값 유효성 검사 및 정규식 검증"));
        m1.leaves.add(new LeafTemplate("권한별 라우팅 및 세션 관리", "관리자/일반 사용자 페이지 접근 권한 격리", "2026-08-30", List.of("Frontend"), "비로그인 접근 차단 및 토큰 만료 핸들러"));
        list.add(m1);

        ModuleTemplate m2 = new ModuleTemplate(
                "핵심 비즈니스 로직 및 데이터 모델",
                "핵심 도메인 엔티티 설계 및 비즈니스 서비스 로직을 구현합니다.",
                "2026-09-15",
                List.of("Backend", "DBA"),
                "주요 트랜잭션 범위(Isolation Level)와 쿼리 성능을 사전에 점검해야 합니다."
        );
        m2.leaves.add(new LeafTemplate("도메인 엔티티 및 스키마 설계", "도메인 테이블 모델링, 외래키 관계 및 인덱스 설정", "2026-09-08", List.of("DBA", "Backend"), "N+1 쿼리 방지 페치 조인 수립"));
        m2.leaves.add(new LeafTemplate("핵심 RESTful API 개발", "명세서 요구사항에 따른 비즈니스 서비스 로직 및 API 구현", "2026-09-15", List.of("Backend"), "스웨거 API 문서화 및 예외 처리 규격 일원화"));
        list.add(m2);

        ModuleTemplate m3 = new ModuleTemplate(
                "사용자 인터랙션 화면 구축",
                "직관적인 화면 UI, 폼 입력 유효성 검사 및 실시간 피드백을 개발합니다.",
                "2026-09-25",
                List.of("Frontend", "Design"),
                "다양한 디바이스 해상도에 최적화된 반응형 레이아웃을 구현해야 합니다."
        );
        m3.leaves.add(new LeafTemplate("반응형 UI 대시보드 및 네비게이션", "사이드바, 탑바 및 메인 레이아웃", "2026-09-20", List.of("Frontend"), "반응형 레이아웃 구성"));
        m3.leaves.add(new LeafTemplate("데이터 입력 폼 및 시각화", "모달 폼, 실시간 유효성 피드백 및 상태 표시", "2026-09-25", List.of("Frontend"), "사용자 인터랙션 개선"));
        list.add(m3);

        ModuleTemplate m4 = new ModuleTemplate(
                "통합 검증 및 배포",
                "전체 기능 엔드투엔드(E2E) 테스트, 성능 튜닝 및 배포 파이프라인을 구축합니다.",
                "2026-09-30",
                List.of("QA", "DevOps"),
                "안정적인 운영을 위한 모니터링 및 롤백 전략을 수립합니다."
        );
        m4.leaves.add(new LeafTemplate("통합 시나리오 검증 및 버그 픽스", "핵심 플로우 회귀 테스트 및 성능 최적화", "2026-09-28", List.of("QA"), "예외 케이스 검증"));
        m4.leaves.add(new LeafTemplate("배포 파이프라인 및 모니터링", "빌드 및 배포 자동화, 서버 헬스체크", "2026-09-30", List.of("DevOps"), "로그 수집 및 알림 연동"));
        list.add(m4);

        return list;
    }

    private String extractRootLabel(String specText, boolean hasFeedback, String feedback) {
        if (hasFeedback && feedback != null && !feedback.isBlank()) {
            // Check if user specified a title in quotes e.g. '제목' or "제목"
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("['\"]([^'\"]+)['\"]").matcher(feedback);
            if (m.find()) {
                String candidate = m.group(1).trim();
                if (!candidate.isBlank()) return truncateOneLine(candidate, 35);
            }
            // Check "X로 바꿔줘" or "X로 변경"
            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("(?:제목|이름|노드|목표)[을를]?\\s*([가-힣a-zA-Z0-9_\\s]+?)(?:[으]로|으로)").matcher(feedback);
            if (m2.find()) {
                String candidate = m2.group(1).trim();
                if (!candidate.isBlank() && candidate.length() < 30) return truncateOneLine(candidate, 35);
            }
        }

        if (specText == null || specText.isBlank()) return "핵심 프로젝트 목표";

        // 1. Check for explicit "프로젝트명: ...", "서비스명: ...", "앱 이름: ..."
        java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile("(?:프로젝트명|서비스명|앱\\s*이름|플랫폼명|주제)[:：]\\s*([^\\n]+)").matcher(specText);
        if (nameMatcher.find()) {
            String found = nameMatcher.group(1).replaceAll("[#*`>]", "").trim();
            if (!found.isBlank()) return truncateOneLine(found, 35);
        }

        // 2. Check if specText has a natural project title line
        for (String line : specText.split("\n")) {
            String trimmed = line.replaceAll("^[#*`>\\s]+", "").replaceAll("^\\d+\\.\\s*", "").trim();
            if (trimmed.isBlank() || trimmed.startsWith("---") || trimmed.contains("목차") 
                    || trimmed.contains("프로젝트 개요") || trimmed.contains("서비스 한 줄 정의") 
                    || trimmed.contains("핵심 가치") || trimmed.contains("문제 정의") || trimmed.contains("요구사항")) {
                continue;
            }
            if (trimmed.length() >= 4) {
                if (trimmed.contains("오케스트레이터")) {
                    return "협업 오케스트레이터 플랫폼";
                }
                if (trimmed.contains("동아리") && trimmed.contains("플랫폼")) {
                    return "대학 동아리 관리 플랫폼 CLUB-IN";
                }
                return truncateOneLine(trimmed, 35);
            }
        }

        String firstLine = specText.split("\n")[0].replaceAll("[#*`>]", "").replaceAll("^\\d+\\.\\s*", "").trim();
        return truncateOneLine(firstLine.isBlank() ? "핵심 프로젝트 목표" : firstLine, 35);
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
