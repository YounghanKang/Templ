package com.example.demo.analysis;

import java.util.List;
import java.util.UUID;

public record AnalysisCommand(
        String projectId,
        String projectName,
        String projectMission,
        UUID eventId,
        String sourceTool,
        String sourceLocation,
        String eventTitle,
        String eventContent,
        String actorName,
        List<AnalysisCandidate> candidates
) {

    public AnalysisCommand {

        candidates =
                candidates == null
                        ? List.of()
                        : List.copyOf(candidates);
    }


    /*
     * 기존 테스트 및 기존 호출부와의 호환성을 위한 생성자.
     *
     * 프로젝트 상세 Context를 전달하지 않는 기존 코드는
     * projectName / projectMission을 null로 둡니다.
     */
    public AnalysisCommand(
            String projectId,
            UUID eventId,
            String sourceTool,
            String sourceLocation,
            String eventTitle,
            String eventContent,
            String actorName,
            List<AnalysisCandidate> candidates
    ) {

        this(
                projectId,
                null,
                null,
                eventId,
                sourceTool,
                sourceLocation,
                eventTitle,
                eventContent,
                actorName,
                candidates
        );
    }
}