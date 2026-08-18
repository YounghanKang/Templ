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

        String rawText = (specText == null || specText.isBlank()) ? "프로젝트 기본 기능 개발 명세서" : specText.trim();
        
        // 1. Root task (tempId: node_root)
        String rootLabel = extractRootLabel(rawText);
        String rootTitle = hasFeedback ? "AI 재생성: " + rootLabel : "최종 목표: " + rootLabel;
        String defaultDueDate = java.time.LocalDate.now().plusMonths(1).toString();
        String rootGoal = rawText;
        String rootAiSummary = hasFeedback 
                ? "사용자 피드백(" + trimmedFeedback + ")을 반영하여 로드맵 트리가 재구성되었습니다. 각 모듈 간 병목을 사전 점검하세요."
                : "명세서 분석을 통해 프로젝트의 단일 진실 공급원(SSOT) 목표가 수립되었습니다. 역할 분담과 선행 의존성을 확인하세요.";

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
        
        // Split specText into meaningful units (by lines, numbered items, or sentences)
        String[] lines = text.split("\n+");
        List<String> validLines = new ArrayList<>();
        for (String line : lines) {
            String clean = line.replaceAll("^[0-9\\.\\-\\s*#]+", "").trim();
            if (clean.length() >= 3) {
                validLines.add(clean);
            }
        }

        if (validLines.size() >= 2) {
            // Case A: User provided multi-line / bullet-point specification
            int modCount = Math.min(validLines.size(), 4);
            for (int i = 0; i < modCount; i++) {
                String lineText = validLines.get(i);
                String modTitle = truncateOneLine(lineText, 25);
                String modGoal = lineText;
                String dueDate = String.format("2026-%02d-%02d", 8 + (i / 2), 15 + (i * 5) % 15);

                ModuleTemplate mod = new ModuleTemplate(
                        modTitle,
                        modGoal,
                        dueDate,
                        List.of("담당자 " + (i + 1), "개발팀"),
                        "'" + modTitle + "' 작업의 명세 요구사항과 산출물 정합성을 검토해야 합니다."
                );

                mod.leaves.add(new LeafTemplate(
                        modTitle + " - 설계 및 인터페이스 정의",
                        "세부 요구사항 정의 및 기초 구조 설계 (" + modTitle + ")",
                        dueDate,
                        List.of("담당자 " + (i + 1)),
                        "초기 설계 누락 방지를 위한 명세 리뷰 필요"
                ));
                mod.leaves.add(new LeafTemplate(
                        modTitle + " - 핵심 기능 구현 및 검증",
                        "실제 기능 구현 및 단위 테스트 완료 (" + modTitle + ")",
                        dueDate,
                        List.of("개발팀"),
                        "단위 테스트 및 예외 처리 케이스 검증"
                ));

                list.add(mod);
            }
        } else {
            // Case B: Single short mission/spec -> Smart Domain Classification & Feature Decomposition
            String lower = text.toLowerCase();
            String topic = extractRootLabel(text);

            if (lower.contains("동아리") || lower.contains("학회") || lower.contains("클럽") || lower.contains("모임") || lower.contains("club")) {
                // 0. Club / Community Portal Domain
                boolean hasKakao = lower.contains("카카오") || lower.contains("kakao");
                boolean hasNaver = lower.contains("네이버") || lower.contains("naver");

                ModuleTemplate m1 = new ModuleTemplate(
                        "소셜 로그인 및 인증 시스템",
                        "OAuth2 기반 소셜 로그인 및 세션/토큰 인증 체계를 구축합니다.",
                        "2026-08-25",
                        List.of("Backend", "Frontend"),
                        "카카오/네이버 인가 코드 교환 및 사용자 이메일/고유식별자 정합성을 검증해야 합니다."
                );
                if (hasKakao) {
                    m1.leaves.add(new LeafTemplate("카카오 로그인 연동", "카카오 OAuth2 SDK 연동 및 사용자 프로필 조회 API", "2026-08-20", List.of("Backend"), "카카오 Redirect URI 및 시크릿 키 관리"));
                }
                if (hasNaver) {
                    m1.leaves.add(new LeafTemplate("네이버 로그인 연동", "네이버 아이디로 로그인(Naver Login) SDK 및 콜백 핸들러", "2026-08-22", List.of("Backend"), "네이버 API 상태 토큰(State) 위변조 방지"));
                }
                if (!hasKakao && !hasNaver) {
                    m1.leaves.add(new LeafTemplate("소셜 로그인 연동", "OAuth2 소셜 계정 연동 및 JWT 토큰 발급", "2026-08-20", List.of("Backend"), "소셜 프로필 동기화"));
                }
                m1.leaves.add(new LeafTemplate("로그인 UI 및 세션 관리", "로그인 버튼 컴포넌트, 자동 로그인 및 로그아웃 처리", "2026-08-25", List.of("Frontend"), "토큰 만료 시 자동 리프레시 처리"));
                list.add(m1);

                ModuleTemplate m2 = new ModuleTemplate(
                        "회원가입 및 부원 관리",
                        "동아리 회원가입 신청, 부원 프로필 및 권한(운영진/일반부원)을 관리합니다.",
                        "2026-09-05",
                        List.of("Fullstack"),
                        "동아리 가입 승인제 적용 시 운영진 검토 큐 및 상태 변경 트랜잭션을 설계해야 합니다."
                );
                m2.leaves.add(new LeafTemplate("동아리 회원가입 폼", "학번/학과/자기소개 입력 및 가입 신청 제출", "2026-08-30", List.of("Frontend"), "입력 폼 유효성 검사"));
                m2.leaves.add(new LeafTemplate("부원 명단 및 권한 관리 API", "운영진/부원 권한 분기, 부원 목록 조회 및 승인/퇴출 API", "2026-09-05", List.of("Backend"), "역할 기반 접근 제어(RBAC) 적용"));
                list.add(m2);

                ModuleTemplate m3 = new ModuleTemplate(
                        "동아리 활동 및 커뮤니티",
                        "공지사항, 자유게시판 및 활동 갤러리/일정 캘린더를 개발합니다.",
                        "2026-09-20",
                        List.of("Frontend", "Backend"),
                        "대용량 사진 업로드 시 S3 Presigned URL 및 썸네일 생성을 고려해야 합니다."
                );
                m3.leaves.add(new LeafTemplate("공지사항 및 게시판 CRUD", "게시글 작성/수정, 댓글 및 첨부파일 업로드", "2026-09-15", List.of("Fullstack"), "게시글 권한 확인 및 XSS 방지"));
                m3.leaves.add(new LeafTemplate("동아리 일정 캘린더 & 갤러리", "정기 모임 일정 등록, D-Day 카운트다운 및 활동 사진 뷰", "2026-09-20", List.of("Frontend"), "반응형 캘린더 컴포넌트"));
                list.add(m3);

            } else if (lower.contains("쇼핑") || lower.contains("커머스") || lower.contains("구매") || lower.contains("상품") || lower.contains("결제") || lower.contains("주문") || lower.contains("shop") || lower.contains("market")) {
                // 1. E-Commerce Domain
                ModuleTemplate m1 = new ModuleTemplate(
                        "회원 및 인증 관리",
                        "소셜 로그인(OAuth2), 회원가입 및 사용자 권한/마이페이지를 구현합니다.",
                        "2026-08-25",
                        List.of("Backend", "Frontend"),
                        "JWT 토큰 탈취 방지 및 리프레시 토큰 로테이션(RTR) 보안 정책을 필수로 적용해야 합니다."
                );
                m1.leaves.add(new LeafTemplate("소셜/이메일 인증 API", "카카오/구글/이메일 기반 로그인 및 토큰 발급", "2026-08-20", List.of("Backend"), "보안 헤더 및 쿠키 정책 검증"));
                m1.leaves.add(new LeafTemplate("마이페이지 및 배송지 관리", "사용자 정보 수정 및 기본 배송지 목록 CRUD", "2026-08-25", List.of("Frontend"), "배송지 주소 검색 우편번호 API 연동"));
                list.add(m1);

                ModuleTemplate m2 = new ModuleTemplate(
                        "상품 카탈로그 및 검색",
                        "카테고리별 상품 탐색, 다차원 필터링 및 상세 페이지를 구축합니다.",
                        "2026-09-05",
                        List.of("Frontend", "Backend", "DBA"),
                        "대량 상품 조회 시 DB 풀스캔을 방지하고 복합 인덱스 및 캐싱(Redis)을 검토해야 합니다."
                );
                m2.leaves.add(new LeafTemplate("상품 목록 및 검색/필터링", "카테고리, 가격대, 브랜드별 다중 필터 쿼리 최적화", "2026-08-30", List.of("Backend", "DBA"), "인덱스 최적화 및 페이징 성능 점검"));
                m2.leaves.add(new LeafTemplate("상품 상세 및 옵션 선택 UI", "이미지 갤러리, 품절 옵션 비활성화 및 반응형 상세 뷰", "2026-09-05", List.of("Frontend"), "옵션 조합별 실시간 재고 연동"));
                list.add(m2);

                ModuleTemplate m3 = new ModuleTemplate(
                        "장바구니 및 PG 결제 연동",
                        "장바구니 담기, 쿠폰/할인 적용 및 PG사 전자결제 연동을 구현합니다.",
                        "2026-09-18",
                        List.of("Backend", "Frontend"),
                        "결제 완료 후 PG사 웹훅 위변조 방지를 위한 서명 검증 및 멱등성 보장이 필수적입니다."
                );
                m3.leaves.add(new LeafTemplate("장바구니 및 주문서 생성", "옵션별 수량 변경, 실시간 총 결제금액 계산", "2026-09-10", List.of("Frontend"), "로컬 스토리지 및 서버 동기화"));
                m3.leaves.add(new LeafTemplate("PG 결제 승인 및 웹훅 처리", "결제 창 연동, 승인 API 호출 및 멱등성 결제 처리", "2026-09-18", List.of("Backend"), "결제 위변조 검증 및 트랜잭션 롤백"));
                list.add(m3);

                ModuleTemplate m4 = new ModuleTemplate(
                        "주문 내역 및 관리자 어드민",
                        "사용자 주문/배송 조회 및 관리자용 주문 상태 변경/통계 대시보드를 개발합니다.",
                        "2026-09-30",
                        List.of("Fullstack", "QA"),
                        "동시 주문 시 재고 음수 방지를 위해 비관적 락(Pessimistic Lock) 또는 원자적 차감을 적용해야 합니다."
                );
                m4.leaves.add(new LeafTemplate("주문 배송 상태 추적", "주문 접수, 배송 중, 완료 단계별 실시간 상태 변경", "2026-09-25", List.of("Backend"), "택배사 송장 연동 API 규격 확인"));
                m4.leaves.add(new LeafTemplate("어드민 매출 및 주문 대시보드", "일별/월별 매출 차트 및 주문 관리 테이블", "2026-09-30", List.of("Frontend"), "대용량 테이블 가상화 및 엑셀 다운로드"));
                list.add(m4);

            } else if (lower.contains("채팅") || lower.contains("메신저") || lower.contains("커뮤니티") || lower.contains("sns") || lower.contains("소셜") || lower.contains("피드") || lower.contains("chat")) {
                // 2. Social / Real-time Messaging Domain
                ModuleTemplate m1 = new ModuleTemplate(
                        "사용자 프로필 및 관계망",
                        "프로필 설정, 친구 추가/팔로우 및 사용자 검색 기능을 개발합니다.",
                        "2026-08-25",
                        List.of("Frontend", "Backend"),
                        "친구 목록 및 팔로워 수 증가에 따른 그래프 쿼리 성능 최적화가 필요합니다."
                );
                m1.leaves.add(new LeafTemplate("프로필 관리 및 아바타 업로드", "사용자 정보 수정 및 S3/CDN 미디어 업로드", "2026-08-20", List.of("Frontend"), "이미지 리사이징 및 CDN 캐시"));
                m1.leaves.add(new LeafTemplate("친구/팔로우 관계 API", "팔로우, 언팔로우, 맞팔로우 여부 확인 API", "2026-08-25", List.of("Backend"), "상호 관계 인덱스 및 알림 이벤트 발행"));
                list.add(m1);

                ModuleTemplate m2 = new ModuleTemplate(
                        "실시간 메시징 & 웹소켓 엔진",
                        "WebSocket/STOMP 기반 1:1 및 그룹 실시간 채팅을 구현합니다.",
                        "2026-09-10",
                        List.of("Backend", "DevOps"),
                        "서버 다중화 시 세션 공유를 위해 Redis Pub/Sub 메시지 브로커 연동이 권장됩니다."
                );
                m2.leaves.add(new LeafTemplate("웹소켓 세션 및 채널 브로드캐스트", "실시간 연결 수립, 메시지 송수신 및 채널 라우팅", "2026-09-02", List.of("Backend"), "재연결(Reconnection) 핸들링"));
                m2.leaves.add(new LeafTemplate("채팅방 UI 및 메시지 가상스크롤", "메시지 버블, 타임스탬프 및 무한 스크롤", "2026-09-10", List.of("Frontend"), "대량 메시지 렌더링 성능 최적화"));
                list.add(m2);

                ModuleTemplate m3 = new ModuleTemplate(
                        "피드 게시물 및 미디어 공유",
                        "타임라인 피드, 게시물 작성/수정, 좋아요 및 댓글을 구현합니다.",
                        "2026-09-20",
                        List.of("Fullstack"),
                        "피드 조회 시 Fan-out-on-write vs Fan-out-on-read 전략을 명확히 선택해야 합니다."
                );
                m3.leaves.add(new LeafTemplate("피드 타임라인 렌더링", "무한 스크롤 및 실시간 인터랙션(좋아요/댓글)", "2026-09-15", List.of("Frontend"), "낙관적 업데이트(Optimistic UI)"));
                m3.leaves.add(new LeafTemplate("미디어 처리 및 댓글 API", "다중 이미지 업로드 및 계층형 대댓글 CRUD", "2026-09-20", List.of("Backend"), "N+1 쿼리 방지 및 이미지 비동기 압축"));
                list.add(m3);

                ModuleTemplate m4 = new ModuleTemplate(
                        "실시간 알림 및 푸시 센터",
                        "새 메시지, 멘션, 댓글에 대한 인앱 실시간 알림 및 푸시를 발송합니다.",
                        "2026-09-30",
                        List.of("Backend", "DevOps"),
                        "SSE(Server-Sent Events) 연결 타임아웃 및 백그라운드 푸시 토큰 유효성을 주기적으로 갱신해야 합니다."
                );
                m4.leaves.add(new LeafTemplate("SSE 실시간 인앱 알림 스트림", "알림 이벤트 수신 및 배지 카운트 갱신", "2026-09-25", List.of("Frontend"), "연결 유실 시 자동 재연결"));
                m4.leaves.add(new LeafTemplate("FCM/웹 푸시 발송 파이프라인", "디바이스 토큰 관리 및 비동기 푸시 큐 발송", "2026-09-30", List.of("Backend"), "푸시 수신 거부 설정 연동"));
                list.add(m4);

            } else if (lower.contains("ai") || lower.contains("llm") || lower.contains("챗봇") || lower.contains("모델") || lower.contains("인공지능") || lower.contains("agent") || lower.contains("gpt")) {
                // 3. AI / LLM Agent Domain
                ModuleTemplate m1 = new ModuleTemplate(
                        "데이터 전처리 및 벡터 DB 구축",
                        "문서 파싱, 청킹(Chunking) 및 벡터 임베딩 파이프라인을 구축합니다.",
                        "2026-08-25",
                        List.of("AI Engineer", "Data"),
                        "청크 사이즈와 오버랩 크기에 따라 RAG 검색 정밀도(Hit Rate)가 크게 달라지므로 벤치마크가 필수입니다."
                );
                m1.leaves.add(new LeafTemplate("문서 파서 및 텍스트 정제", "PDF/Word/Markdown 텍스트 추출 및 정규화", "2026-08-20", List.of("Data"), "노이즈 제거 및 메타데이터 추출"));
                m1.leaves.add(new LeafTemplate("벡터 임베딩 및 인덱싱", "OpenAI 임베딩 생성 및 Pinecone/Chroma 인덱싱", "2026-08-25", List.of("AI Engineer"), "유사도 검색(Cosine Sim) 최적화"));
                list.add(m1);

                ModuleTemplate m2 = new ModuleTemplate(
                        "프롬프트 체이닝 및 LLM 서빙",
                        "시스템 프롬프트 설계, LangChain/LangGraph 에이전트 및 LLM API를 연동합니다.",
                        "2026-09-10",
                        List.of("Backend", "AI Engineer"),
                        "LLM API 호출 실패 시 자동 재시도 및 폴백 모델(Fallback Model) 서킷 브레이커를 구성해야 합니다."
                );
                m2.leaves.add(new LeafTemplate("RAG 맥락 검색 및 프롬프트 합성", "관련 문서 유사도 쿼리 및 동적 프롬프트 조립", "2026-09-02", List.of("AI Engineer"), "할루시네이션 완화 프롬프트 검증"));
                m2.leaves.add(new LeafTemplate("LLM 스트리밍 API 게이트웨이", "SSE 기반 토큰 스트리밍 응답 엔드포인트 구현", "2026-09-10", List.of("Backend"), "토큰 소비량 실시간 추적"));
                list.add(m2);

                ModuleTemplate m3 = new ModuleTemplate(
                        "대화형 챗봇 인터페이스",
                        "타이핑 스트리밍 효과, 마크다운 렌더링, 코드 블록 복사 및 재생성 UI를 구현합니다.",
                        "2026-09-20",
                        List.of("Frontend"),
                        "수식(KaTeX) 및 코드 블록이 포함된 스트리밍 텍스트 렌더링 시 깜빡임(Flickering)을 최소화해야 합니다."
                );
                m3.leaves.add(new LeafTemplate("스트리밍 챗 UI 및 마크다운 파서", "토큰 단위 실시간 텍스트 렌더링 및 코드 하이라이팅", "2026-09-15", List.of("Frontend"), "렌더링 성능 최적화"));
                m3.leaves.add(new LeafTemplate("대화 세션 및 히스토리 관리", "세션별 대화 저장, 제목 자동 생성 및 삭제", "2026-09-20", List.of("Frontend", "Backend"), "세션 히스토리 압축"));
                list.add(m3);

                ModuleTemplate m4 = new ModuleTemplate(
                        "토큰 과금 및 프롬프트 평가 모니터링",
                        "사용자별 토큰 사용량 측정, 크레딧 차감 및 응답 품질 로깅을 구축합니다.",
                        "2026-09-30",
                        List.of("Backend", "DevOps"),
                        "비정상적 대량 요청에 대한 Rate Limiting(IP/계정별)을 게이트웨이 단에서 차단해야 합니다."
                );
                m4.leaves.add(new LeafTemplate("사용량 측정 및 크레딧 차감", "요청/응답 토큰 카운트 및 사용자 잔여 크레딧 갱신", "2026-09-25", List.of("Backend"), "동시 요청 크레딧 원자적 차감"));
                m4.leaves.add(new LeafTemplate("응답 피드백 및 모니터링 대시보드", "좋아요/싫어요 수집 및 Langfuse/로그 분석", "2026-09-30", List.of("DevOps"), "프롬프트 평가 지표 추적"));
                list.add(m4);

            } else {
                // 4. General Core Feature Architecture Domain
                ModuleTemplate m1 = new ModuleTemplate(
                        "사용자 인증 및 보안 체계",
                        "안전한 계정 인증(JWT), 세션 관리 및 역할 기반 권한 제어(RBAC)를 구축합니다.",
                        "2026-08-30",
                        List.of("Backend", "Security"),
                        "인증 토큰 탈취 방지 및 API 엔드포인트별 인가 인터셉터를 철저히 검증해야 합니다."
                );
                m1.leaves.add(new LeafTemplate("회원가입 및 JWT 인증 API", "비밀번호 단방향 암호화(BCrypt) 및 토큰 발급", "2026-08-25", List.of("Backend"), "입력값 유효성 검사 및 정규식 검증"));
                m1.leaves.add(new LeafTemplate("권한별 라우팅 및 세션 관리", "관리자/일반 사용자 페이지 접근 권한 격리", "2026-08-30", List.of("Frontend"), "비로그인 접근 차단 및 토큰 만료 핸들러"));
                list.add(m1);

                ModuleTemplate m2 = new ModuleTemplate(
                        topic + " 핵심 기능 및 데이터 모델",
                        "'" + topic + "'의 주요 비즈니스 로직과 CRUD 데이터베이스 스키마를 구현합니다.",
                        "2026-09-15",
                        List.of("Backend", "DBA"),
                        "주요 트랜잭션 범위(Isolation Level)와 쿼리 성능을 사전에 점검해야 합니다."
                );
                m2.leaves.add(new LeafTemplate("핵심 도메인 엔티티 및 스키마 설계", "도메인 테이블 모델링, 외래키 관계 및 인덱스 설정", "2026-09-08", List.of("DBA", "Backend"), "N+1 쿼리 방지 페치 조인 수립"));
                m2.leaves.add(new LeafTemplate("핵심 비즈니스 RESTful API 개발", "명세서 요구사항에 따른 비즈니스 서비스 로직 및 API 구현", "2026-09-15", List.of("Backend"), "스웨거 API 문서화 및 예외 처리 규격 일원화"));
                list.add(m2);

                ModuleTemplate m3 = new ModuleTemplate(
                        "사용자 인터랙션 화면 구축",
                        "직관적인 대시보드 화면, 폼 입력 유효성 검사 및 실시간 UI 피드백을 개발합니다.",
                        "2026-09-25",
                        List.of("Frontend", "UI Designer"),
                        "다양한 화면 해상도에 대응하는 반응형 레이아웃과 상태 동기화가 권장됩니다."
                );
                m3.leaves.add(new LeafTemplate("반응형 대시보드 및 네비게이션", "사이드바, 탑바 및 메인 콘텐츠 반응형 그리드", "2026-09-20", List.of("Frontend"), "모바일/태블릿 반응형 레이아웃"));
                m3.leaves.add(new LeafTemplate("데이터 입력 폼 및 시각화", "모달 폼, 실시간 유효성 피드백 및 통계 차트", "2026-09-25", List.of("Frontend"), "토스트 알림 및 로딩 스피너 처리"));
                list.add(m3);

                ModuleTemplate m4 = new ModuleTemplate(
                        "통합 검증 및 프로덕션 배포",
                        "전체 기능 엔드투엔드(E2E) 테스트, 성능 튜닝 및 CI/CD 배포 파이프라인을 구축합니다.",
                        "2026-09-30",
                        List.of("QA", "DevOps"),
                        "배포 후 장애 발생 시 즉시 이전 버전으로 롤백할 수 있는 무중단 배포 전략을 수립해야 합니다."
                );
                m4.leaves.add(new LeafTemplate("통합 시나리오 검증 및 버그 픽스", "사용자 핵심 플로우 회귀 테스트 및 성능 최적화", "2026-09-28", List.of("QA"), "예외 케이스 및 동시성 검증"));
                m4.leaves.add(new LeafTemplate("CI/CD 파이프라인 및 모니터링", "도커 컨테이너 빌드, 자동 배포 및 서버 헬스체크", "2026-09-30", List.of("DevOps"), "로그 수집 및 알림 웹훅 연동"));
                list.add(m4);
            }
        }

        if (hasFeedback) {
            String fbLower = feedback.toLowerCase();
            boolean isDeleteRequest = fbLower.contains("삭제") || fbLower.contains("제거") || fbLower.contains("없애") || fbLower.contains("빼") || fbLower.contains("줄여") || fbLower.contains("중복");

            if (isDeleteRequest) {
                // Remove matching leaves or modules
                String[] keywords = {"단체톡", "채팅", "카카오", "네이버", "회원가입", "게시판", "알림", "결제", "통계", "소셜"};
                for (String kw : keywords) {
                    if (fbLower.contains(kw)) {
                        for (ModuleTemplate mod : list) {
                            mod.leaves.removeIf(l -> l.label.contains(kw) || l.goal.contains(kw));
                        }
                        list.removeIf(m -> (m.label.contains(kw) || m.goal.contains(kw)) && m.leaves.isEmpty());
                    }
                }
            } else {
                String fbClean = truncateOneLine(feedback, 25);
                ModuleTemplate mFeedback = new ModuleTemplate(
                        "피드백 반영: " + fbClean,
                        "사용자 추가 요청 사항(" + feedback + ")을 WBS에 반영하여 개발 및 검증을 진행합니다.",
                        java.time.LocalDate.now().plusMonths(1).toString(),
                        List.of("PM", "담당팀"),
                        "피드백 요구사항에 따른 변경점과 기존 작업 간의 정합성을 최우선으로 검증해야 합니다."
                );
                mFeedback.leaves.add(new LeafTemplate(
                        fbClean + " 기능 구현",
                        "피드백 요구사항 구체화 및 기능 구현 (" + feedback + ")",
                        java.time.LocalDate.now().plusDays(20).toString(),
                        List.of("담당 개발자"),
                        "요구사항 세부 스펙 일치 여부 확인"
                ));
                mFeedback.leaves.add(new LeafTemplate(
                        fbClean + " 검증 및 통합",
                        "피드백 반영 기능 단위 테스트 및 통합 시나리오 검증",
                        java.time.LocalDate.now().plusMonths(1).toString(),
                        List.of("QA"),
                        "기존 기능과의 회귀 테스트 필수"
                ));
                list.add(mFeedback);
            }
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
