package com.example.demo.analysis;

import java.util.List;
import java.util.UUID;

public record AnalysisCandidate(
        UUID nodeId,
        String title,
        int matchScore,
        List<String> matchedTerms
) {

    public AnalysisCandidate {

        matchedTerms =
                matchedTerms == null
                        ? List.of()
                        : List.copyOf(matchedTerms);
    }
}


