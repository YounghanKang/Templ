package com.example.demo.analysis;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.matching.NodeMatchingResult;
import com.example.demo.matching.NodeMatchingService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AnalyzeEventCore {

    private static final int SOFT_WARNING_MIN_SCORE = 6;

    private static final int HARD_WARNING_MIN_SCORE = 8;


    private final NodeMatchingService nodeMatchingService;

    private final AnalysisContextBuilder analysisContextBuilder;

    private final ContextAnalysisPort contextAnalysisPort;

    private final AnalysisResultValidator analysisResultValidator;


    public AnalyzeExecutionOutcome execute(
            CollaborationEvent event
    ) {

        if (event == null) {

            throw new IllegalArgumentException(
                    "event는 필수입니다."
            );
        }


        /*
         * 1. 제목 + 본문을 하나의 매칭 텍스트로 구성
         */
        String eventText =
                buildEventText(
                        event.getTitle(),
                        event.getContent()
                );


        /*
         * 2. Backend1 후보 Node
         *    → Backend2 규칙 기반 Top 5 매칭
         */
        NodeMatchingResult matchingResult =
                nodeMatchingService.match(
                        event.getProjectId(),
                        event.getSourceLocation(),
                        eventText
                );


        /*
         * 후보 Node가 하나도 없다면
         * AI 호출 자체를 하지 않습니다.
         */
        if (!matchingResult.mapped()) {

            return AnalyzeExecutionOutcome.unmapped(
                    matchingResult
            );
        }


        /*
         * 3. Event + 후보 Task를
         *    AI용 AnalysisCommand로 변환
         */
        AnalysisCommand command =
                analysisContextBuilder.build(
                        event,
                        matchingResult
                );


        /*
         * 4. AI 호출
         *
         * 지금 테스트에서는 Fake Port가 호출됩니다.
         * 실제 OpenAI는 나중 Adapter에서 연결합니다.
         */
        ContextAnalysisResult analysisResult =
                contextAnalysisPort.analyze(
                        command
                );


        /*
         * 5. AI 결과 검증
         *
         * riskScore
         * confidence
         * primaryNodeId
         * 등을 검증합니다.
         */
        analysisResultValidator.validate(
                command,
                analysisResult
        );


        /*
         * 6. UI 경고 레벨 결정
         */
        AnalysisOutcomeType outcomeType =
                determineOutcomeType(
                        analysisResult
                );


        return AnalyzeExecutionOutcome.analyzed(
                outcomeType,
                matchingResult,
                command,
                analysisResult
        );
    }


    private AnalysisOutcomeType determineOutcomeType(
            ContextAnalysisResult result
    ) {

        if (!result.meaningfulChange()) {

            return AnalysisOutcomeType.NO_WARNING;
        }


        if (
                result.riskScore()
                        >= HARD_WARNING_MIN_SCORE
        ) {

            return AnalysisOutcomeType.HARD_WARNING;
        }


        if (
                result.riskScore()
                        >= SOFT_WARNING_MIN_SCORE
        ) {

            return AnalysisOutcomeType.SOFT_WARNING;
        }


        return AnalysisOutcomeType.NO_WARNING;
    }


    private String buildEventText(
            String title,
            String content
    ) {

        String safeTitle =
                title == null
                        ? ""
                        : title.trim();


        String safeContent =
                content == null
                        ? ""
                        : content.trim();


        if (safeTitle.isBlank()) {
            return safeContent;
        }


        if (safeContent.isBlank()) {
            return safeTitle;
        }


        return safeTitle
                + "\n"
                + safeContent;
    }
}