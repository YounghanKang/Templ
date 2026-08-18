package com.example.demo.analysis;

import java.util.List;

public record AnalysisCandidate(
        String nodeId,
        String title,
        String goal,
        String aiSummary,
        String status,
        Integer progress,
        List<String> assignees,
        List<String> prerequisites,
        String dueDate,
        int matchScore,
        List<String> matchedTerms
) {

    public AnalysisCandidate {

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
     * 기존 테스트와 기존 호출부의
     * 4개 인자 생성자를 계속 지원합니다.
     */
    public AnalysisCandidate(
            String nodeId,
            String title,
            int matchScore,
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
                matchScore,
                matchedTerms
        );
    }
}