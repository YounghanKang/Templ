package com.example.demo.analysis;

import com.example.demo.matching.NodeMatchingResult;

public record AnalyzeExecutionOutcome(
        AnalysisOutcomeType outcomeType,
        NodeMatchingResult matchingResult,
        AnalysisCommand command,
        ContextAnalysisResult analysisResult
) {

    public static AnalyzeExecutionOutcome unmapped(
            NodeMatchingResult matchingResult
    ) {

        return new AnalyzeExecutionOutcome(
                AnalysisOutcomeType.UNMAPPED,
                matchingResult,
                null,
                null
        );
    }


    public static AnalyzeExecutionOutcome analyzed(
            AnalysisOutcomeType outcomeType,
            NodeMatchingResult matchingResult,
            AnalysisCommand command,
            ContextAnalysisResult analysisResult
    ) {

        return new AnalyzeExecutionOutcome(
                outcomeType,
                matchingResult,
                command,
                analysisResult
        );
    }
}

/*ex위험도8정상분석
* outcomeType     = HARD_WARNING
matchingResult  = 관련 Task Top 5
command         = AI에게 보낸 데이터
analysisResult  = AI가 돌려준 검증 완료 결과*/

/*ex관련task없으면
* outcomeType     = UNMAPPED
matchingResult  = 후보 없음
command         = null
analysisResult  = null*/