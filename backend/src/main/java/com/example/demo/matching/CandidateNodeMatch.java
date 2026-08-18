package com.example.demo.matching;

import java.util.List;

public record CandidateNodeMatch(
        String nodeId,
        String title,
        String goal,
        String aiSummary,
        String status,
        Integer progress,
        List<String> assignees,
        List<String> prerequisites,
        String dueDate,
        int score,
        List<String> matchedTerms
) {

    public CandidateNodeMatch {

        assignees =
                assignees == null
                        ? List.of()
                        : List.copyOf(assignees);

        prerequisites =
                prerequisites == null
                        ? List.of()
                        : List.copyOf(prerequisites);

        matchedTerms =
                matchedTerms == null
                        ? List.of()
                        : List.copyOf(matchedTerms);
    }


    /*
     * 기존 테스트와 기존 코드의
     * 4개 인자 생성자를 계속 지원합니다.
     */
    public CandidateNodeMatch(
            String nodeId,
            String title,
            int score,
            List<String> matchedTerms
    ) {

        this(
                nodeId,
                title,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                score,
                matchedTerms
        );
    }
}

/*
 * 참고
 *
 * Candidate Match Score
 * → 어떤 Task가 메시지와 관련 있는지를
 *   규칙 기반으로 정렬하기 위한 내부 점수
 *
 * AI Risk Score
 * → 실제 충돌/변경 위험도 0~10
 */