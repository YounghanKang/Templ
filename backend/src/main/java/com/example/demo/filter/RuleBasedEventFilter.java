package com.example.demo.filter;

import com.example.demo.collaboration.CollaborationEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RuleBasedEventFilter {

    private static final Set<String> IMPORTANT_KEYWORDS =
            Set.of(
                    "추가",
                    "삭제",
                    "변경",
                    "수정",
                    "취소",
                    "확정",
                    "연기",
                    "충돌",
                    "중복",
                    "담당",
                    "담당자",
                    "마감",
                    "일정",
                    "api",
                    "schema",
                    "스키마",
                    "deploy",
                    "배포",
                    "spec",
                    "스펙",
                    "scope",
                    "범위"
            );


    private static final Set<String> LOW_VALUE_MESSAGES =
            Set.of(
                    "감사합니다",
                    "감사해요",
                    "고마워요",
                    "고마워",
                    "확인했습니다",
                    "확인했어요",
                    "확인",
                    "넵",
                    "네",
                    "ㅇㅋ",
                    "오케이",
                    "ok",
                    "okay",
                    "ㅎㅎ",
                    "ㅋㅋ",
                    "굿"
            );


    public FilterResult evaluate(
            CollaborationEvent event
    ) {

        return evaluate(
                event.getContent(),
                event.isGeneratedBySystem()
        );
    }


    public FilterResult evaluate(
            String content,
            boolean generatedBySystem
    ) {

        /*
         * 1. 우리 시스템/Bot이 만든 메시지
         */
        if (generatedBySystem) {

            return FilterResult.filteredOut(
                    "GENERATED_BY_SYSTEM",
                    "시스템 또는 Bot이 생성한 이벤트입니다."
            );
        }


        /*
         * 2. 빈 메시지
         */
        String text =
                normalize(content);

        if (text.isBlank()) {

            return FilterResult.filteredOut(
                    "EMPTY_CONTENT",
                    "분석할 내용이 없습니다."
            );
        }


        /*
         * 3. 단순 응답
         */
        if (LOW_VALUE_MESSAGES.contains(text)) {

            return FilterResult.filteredOut(
                    "LOW_VALUE_MESSAGE",
                    "단순 확인·감사·반응 메시지입니다."
            );
        }


        /*
         * 4. 중요 키워드 검사
         */
        List<String> matchedKeywords =
                findImportantKeywords(text);

        if (!matchedKeywords.isEmpty()) {

            int score =
                    Math.min(
                            10,
                            5 + matchedKeywords.size()
                    );

            return FilterResult.aiRequired(
                    score,
                    matchedKeywords,
                    "프로젝트 변경 가능성을 나타내는 키워드가 포함되어 있습니다."
            );
        }


        /*
         * 5. 짧지만 중요한 키워드가 없는 메시지
         */
        if (text.length() < 10) {

            return FilterResult.filteredOut(
                    "SHORT_LOW_VALUE",
                    "짧고 중요 변화 키워드가 없는 메시지입니다."
            );
        }


        /*
         * 6. 나머지는 기록만
         */
        return FilterResult.logOnly(
                1,
                List.of("GENERAL_ACTIVITY"),
                "일반 협업 메시지로 기록하지만 AI 분석은 수행하지 않습니다."
        );
    }


    private String normalize(
            String content
    ) {

        if (content == null) {
            return "";
        }

        return content
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }


    private List<String> findImportantKeywords(
            String text
    ) {

        List<String> matched =
                new ArrayList<>();

        for (String keyword : IMPORTANT_KEYWORDS) {

            if (
                    text.contains(
                            keyword.toLowerCase(
                                    Locale.ROOT
                            )
                    )
            ) {
                matched.add(keyword);
            }
        }

        return matched;
    }
}