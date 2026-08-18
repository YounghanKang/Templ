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

                Candidate Nodes에는 프로젝트에서 계획된 Task 정보가 포함되어 있습니다.
                EVENT는 Slack 또는 GitHub에서 관찰된 실제 협업 활동입니다.

                반드시 계획된 Task 정보와 실제 협업 활동을 비교해서 판단하세요.

                특히 다음 차이를 확인하세요.
                - 계획된 목표(goal)와 실제 수행 내용의 차이
                - 계획된 상태(status) 및 진행률(progress)과 실제 진행 상황의 차이
                - 담당자(assignees)와 실제 작업자의 불일치
                - 선행 작업(prerequisites)을 무시한 진행 여부
                - 마감일(dueDate)과 충돌하는 일정 위험
                - 기존 AI 요약(aiSummary)과 실제 활동의 불일치

                단순히 EVENT 내용을 요약하는 것은 변경 감지가 아닙니다.
                계획과 실제 사이에 의미 있는 차이가 있는지를 중심으로 판단하세요.

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
                아래 Candidate Nodes에 실제로 존재하는 nodeId만 사용하세요.
                존재하지 않는 nodeId를 새로 만들어내지 마세요.

                응답은 설명이나 Markdown 없이
                JSON 객체 하나만 반환하세요.

                JSON 형식:
                {
                  "meaningfulChange": true,
                  "changeType": "SCOPE_DRIFT",
                  "summary": "변경 요약",
                  "riskScore": 7,
                  "confidence": 0.90,
                  "taskId": "nodeId 또는 null",
                  "impactedNodeIds": [],
                  "proposedActions": [],
                  "openQuestions": []
                }

                [PROJECT CONTEXT]

                projectName:
                %s

                projectMission:
                %s

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
                safe(command.projectName()),
                safe(command.projectMission()),
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
              goal: %s
              aiSummary: %s
              status: %s
              progress: %s
              assignees: %s
              prerequisites: %s
              dueDate: %s
              matchScore: %d
              matchedTerms: %s
            """.formatted(
                candidate.nodeId(),
                safe(candidate.title()),
                safe(candidate.goal()),
                safe(candidate.aiSummary()),
                safe(candidate.status()),
                safe(candidate.progress()),
                candidate.assignees(),
                candidate.prerequisites(),
                safe(candidate.dueDate()),
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