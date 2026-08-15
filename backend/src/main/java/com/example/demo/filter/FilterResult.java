package com.example.demo.filter;

import java.util.List;

public record FilterResult(
        FilterDecision decision,
        int score,
        List<String> matchedRules,
        String reason,
        String filterVersion
) {

    private static final String CURRENT_VERSION =
            "filter-1.0.0";


    public static FilterResult filteredOut(
            String rule,
            String reason
    ) {
        return new FilterResult(
                FilterDecision.FILTERED_OUT,
                0,
                List.of(rule),
                reason,
                CURRENT_VERSION
        );
    }


    public static FilterResult logOnly(
            int score,
            List<String> matchedRules,
            String reason
    ) {
        return new FilterResult(
                FilterDecision.LOG_ONLY,
                score,
                matchedRules,
                reason,
                CURRENT_VERSION
        );
    }


    public static FilterResult aiRequired(
            int score,
            List<String> matchedRules,
            String reason
    ) {
        return new FilterResult(
                FilterDecision.AI_REQUIRED,
                score,
                matchedRules,
                reason,
                CURRENT_VERSION
        );
    }
}

/* 두 값은 다른 값
Filter score
→ AI를 호출할지 결정하기 위한 규칙 점수

AI riskScore
→ 실제 Task 충돌 위험도 1~10
* */