package com.example.demo.analysis.openai;

import com.example.demo.analysis.AnalysisCandidate;
import com.example.demo.analysis.AnalysisCommand;

import java.util.stream.Collectors;

public class OpenAiPromptBuilder {

    public String build(
            AnalysisCommand command
    ) {

        if (command == null) {
            throw new IllegalArgumentException(
                    "command는 필수입니다."
            );
        }


        String candidatesText =
                command.candidates()
                        .stream()
                        .map(this::formatCandidate)
                        .collect(
                                Collectors.joining(
                                        "\n"
                                )
                        );


        if (candidatesText.isBlank()) {
            candidatesText = "(후보 노드 없음)";
        }


        return """
                당신은 협업 프로젝트의 변경 감지 분석기입니다.

                사용자의 메시지를 단순 요약하지 말고,
                현재 프로젝트 구조에 영향을 주는 의미 있는 변경인지 판단하세요.

                반드시 아래 7가지 changeType 중 하나만 사용하세요.

                - SCOPE_DRIFT
                - DUPLICATE_WORK
                - OWNERSHIP_COLLISION
                - DEPENDENCY_BREAK
                - DECISION_CONTRADICTION
                - DELIVERABLE_MISMATCH
                - DEADLINE_COLLISION

                의미 있는 변경이 아니라면:
                - meaningfulChange = false
                - changeType = null
                - riskScore는 0~3 범위
                - taskId는 null이어도 됩니다.

                의미 있는 변경이라면:
                - meaningfulChange = true
                - changeType은 반드시 위 7개 중 하나
                - riskScore는 0~10
                - confidence는 0.0~1.0
                - taskId는 제공된 후보 노드 중 가장 직접적인 노드
                - impactedNodeIds는 추가로 영향받는 후보 노드 ID
                - proposedActions에는 프로젝트 수정 제안만 작성
                - 실제 프로젝트를 직접 변경했다고 표현하지 마세요.
                - 확실하지 않은 내용은 openQuestions에 작성하세요.

                중요:
                taskId와 impactedNodeIds에는
                아래 Candidate Nodes에 실제로 존재하는 UUID만 사용하세요.

                응답은 설명이나 Markdown 없이
                JSON 객체 하나만 반환하세요.

                JSON 형식:
                {
                  "meaningfulChange": true,
                  "changeType": "SCOPE_DRIFT",
                  "summary": "변경 요약",
                  "riskScore": 7,
                  "confidence": 0.90,
                  "taskId": "UUID 또는 null",
                  "impactedNodeIds": [],
                  "proposedActions": [],
                  "openQuestions": []
                }

                [EVENT]

                projectId:
                %s

                eventId:
                %s

                sourceTool:
                %s

                sourceLocation:
                %s

                eventTitle:
                %s

                eventContent:
                %s

                actorName:
                %s


                [CANDIDATE NODES]

                %s
                """.formatted(
                safe(command.projectId()),
                safe(command.eventId()),
                safe(command.sourceTool()),
                safe(command.sourceLocation()),
                safe(command.eventTitle()),
                safe(command.eventContent()),
                safe(command.actorName()),
                candidatesText
        );
    }


    private String formatCandidate(
            AnalysisCandidate candidate
    ) {

        return """
                - nodeId: %s
                  title: %s
                  matchScore: %d
                  matchedTerms: %s
                """.formatted(
                candidate.nodeId(),
                safe(candidate.title()),
                candidate.matchScore(),
                candidate.matchedTerms()
        );
    }


    private String safe(
            Object value
    ) {

        return value == null
                ? "(없음)"
                : value.toString();
    }
}