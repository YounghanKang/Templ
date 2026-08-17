package com.example.demo.matching;

import java.util.List;
import java.util.UUID;

public record CandidateNodeMatch(
        String nodeId,
        String title,
        int score,
        List<String> matchedTerms
) {

    public CandidateNodeMatch {

        matchedTerms =
                matchedTerms == null
                        ? List.of()
                        : List.copyOf(matchedTerms);
    }
}

/*
* 참고
* Candidate Match Score
→ 어떤 Task가 메시지와 관련 있는지를
  규칙 기반으로 정렬하기 위한 내부 점수

AI Risk Score
→ 실제 충돌/변경 위험도 1~10
* */