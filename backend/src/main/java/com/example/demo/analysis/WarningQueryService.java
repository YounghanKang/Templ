package com.example.demo.analysis;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.collaboration.CollaborationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class WarningQueryService {

    private final CollaborationEventRepository eventRepository;
    private final EventAnalysisRepository analysisRepository;

    public WarningQueryService(
            CollaborationEventRepository eventRepository,
            EventAnalysisRepository analysisRepository
    ) {
        this.eventRepository = eventRepository;
        this.analysisRepository = analysisRepository;
    }

    /**
     * 프로젝트의 각 CollaborationEvent에 대해
     * 가장 최신 분석 결과만 확인합니다.
     *
     * SOFT_WARNING / HARD_WARNING만 반환하며,
     * NO_WARNING / UNMAPPED 결과는 API 응답에서 제외합니다.
     */
    @Transactional(readOnly = true)
    public List<WarningResponse> findWarnings(
            String projectId
    ) {

        return eventRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::findLatestWarning)
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparing(
                                WarningResponse::analyzedAt
                        ).reversed()
                )
                .toList();
    }

    private WarningResponse findLatestWarning(
            CollaborationEvent event
    ) {

        EventAnalysis analysis =
                analysisRepository
                        .findTopByCollaborationEventIdOrderByCreatedAtDesc(
                                event.getId()
                        )
                        .orElse(null);

        if (analysis == null) {
            return null;
        }

        if (
                analysis.getOutcomeType()
                        != AnalysisOutcomeType.SOFT_WARNING
                        &&
                analysis.getOutcomeType()
                        != AnalysisOutcomeType.HARD_WARNING
        ) {
            return null;
        }

        return new WarningResponse(
                analysis.getId(),
                event.getId(),
                event.getProjectId(),
                analysis.getTaskId(),
                analysis.getOutcomeType(),
                analysis.getChangeType(),
                analysis.getSummary(),
                analysis.getRiskScore(),
                analysis.getConfidence(),
                event.getSourceTool().name(),
                event.getSourceLocation(),
                event.getSourceUrl(),
                event.getOccurredAt(),
                analysis.getCreatedAt()
        );
    }
}
