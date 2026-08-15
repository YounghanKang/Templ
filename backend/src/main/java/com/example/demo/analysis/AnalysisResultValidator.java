package com.example.demo.analysis;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AnalysisResultValidator {

    public void validate(
            AnalysisCommand command,
            ContextAnalysisResult result
    ) {

        if (command == null) {

            throw new IllegalArgumentException(
                    "command는 필수입니다."
            );
        }


        if (result == null) {

            throw new IllegalArgumentException(
                    "analysis result는 필수입니다."
            );
        }


        validateRiskScore(
                result.riskScore()
        );


        validateConfidence(
                result.confidence()
        );


        /*
         * 의미 있는 변경이 아니면
         * changeType / primaryNode가 없어도 허용합니다.
         */
        if (!result.meaningfulChange()) {
            return;
        }


        if (result.changeType() == null) {

            throw new IllegalArgumentException(
                    "의미 있는 변경에는 changeType이 필요합니다."
            );
        }


        if (
                result.summary() == null
                        || result.summary().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "의미 있는 변경에는 summary가 필요합니다."
            );
        }


        if (result.taskId() == null) {

            throw new IllegalArgumentException(
                    "의미 있는 변경에는 taskId가 필요합니다."
            );
        }


        /*
         * AI가 존재하지 않는 Node UUID를
         * 만들어 내는 것을 방지합니다.
         */
        Set<UUID> allowedCandidateIds =
                command
                        .candidates()
                        .stream()
                        .map(
                                AnalysisCandidate::nodeId
                        )
                        .collect(
                                Collectors.toSet()
                        );


        if (
                !allowedCandidateIds.contains(
                        result.taskId()
                )
        ) {

            throw new IllegalArgumentException(
                    "taskId가 후보 Node에 존재하지 않습니다."
            );
        }
    }


    private void validateRiskScore(
            int riskScore
    ) {

        if (
                riskScore < 1
                        || riskScore > 10
        ) {

            throw new IllegalArgumentException(
                    "riskScore는 1 이상 10 이하여야 합니다."
            );
        }
    }


    private void validateConfidence(
            double confidence
    ) {

        if (
                confidence < 0.0
                        || confidence > 1.0
        ) {

            throw new IllegalArgumentException(
                    "confidence는 0 이상 1 이하여야 합니다."
            );
        }
    }
}